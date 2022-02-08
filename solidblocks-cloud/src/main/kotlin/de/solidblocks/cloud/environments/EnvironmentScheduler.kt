package de.solidblocks.cloud.environments

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.CloudConstants.ENVIRONMENT_HEALTHCHECK_INTERVAL
import de.solidblocks.cloud.ProvisionerContext
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import java.time.Instant
import java.util.*
import javax.sql.DataSource


class EnvironmentScheduler(private val dataSource: DataSource, val environmentsRepository: EnvironmentsRepository, val provisionerContext: ProvisionerContext, val lockingTaskExecutor: LockingTaskExecutor) {

    private val jacksonObjectMapper = jacksonObjectMapper()

    private val logger = KotlinLogging.logger {}

    fun startScheduler() {

        val applyTask: OneTimeTask<String> = Tasks.oneTime("environments-apply", String::class.java).execute { inst, _ ->
            val reference = jacksonObjectMapper.readValue(inst.data, EnvironmentReference::class.java)
            lockingTaskExecutor.executeWithLock<Any>({
                provisionerContext.createEnvironmentProvisioner(reference).apply()
            }, LockConfiguration("${reference}", Instant.now().plusSeconds(600)))
        }


        val healthCheckTask =
            Tasks.recurring("environments-healthcheck", FixedDelay.of(ENVIRONMENT_HEALTHCHECK_INTERVAL))
                .execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
                    run {
                        environmentsRepository.listEnvironments().forEach {
                            val provisioner = provisionerContext.createEnvironmentProvisioner(it.reference)
                            lockingTaskExecutor.executeWithLock<Any>({
                                if (!provisioner.healthcheck()) {
                                    val client = ctx.schedulerClient
                                    client.schedule(
                                        applyTask.instance(
                                            UUID.randomUUID().toString(),
                                            jacksonObjectMapper.writeValueAsString(it.reference)
                                        ), Instant.now()
                                    )
                                }
                            }, LockConfiguration("${it.reference}", Instant.now().plusSeconds(600)))
                        }
            }
        }


        val scheduler = Scheduler.create(dataSource, applyTask).startTasks(healthCheckTask).threads(5).build()
        scheduler.start()
    }
}
