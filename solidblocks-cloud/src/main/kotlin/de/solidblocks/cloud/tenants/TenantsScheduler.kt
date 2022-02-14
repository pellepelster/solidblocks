package de.solidblocks.cloud.tenants

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.base.reference.TenantReference
import de.solidblocks.cloud.ProvisionerContext
import de.solidblocks.cloud.environments.EnvironmentsStatusManager
import de.solidblocks.cloud.model.repositories.TenantsRepository
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class TenantsScheduler(
    private val dataSource: DataSource,
    val provisionerContext: ProvisionerContext,
    private val lockingTaskExecutor: LockingTaskExecutor,
    private val tenantsRepository: TenantsRepository,
    private val tenantsStatusManager: TenantsStatusManager,
    private val environmentsStatusManager: EnvironmentsStatusManager
) {

    private val logger = KotlinLogging.logger {}

    private val jacksonObjectMapper = jacksonObjectMapper()

    private val scheduler: Scheduler

    private val applyTask: OneTimeTask<String> = Tasks.oneTime("tenants-apply", String::class.java).execute { inst, _ ->
        val reference = jacksonObjectMapper.readValue(inst.data, TenantReference::class.java)
        lockingTaskExecutor.executeWithLock<Any>({
            provisionerContext.createTenantProvisioner(reference).apply()
        }, LockConfiguration("$reference", Instant.now().plusSeconds(600)))
        }

        private val healthCheckTask = Tasks.recurring("tenants-healthcheck", FixedDelay.ofSeconds(15))
            .execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
                run {

                    tenantsRepository.listTenants().forEach {
                        if (!environmentsStatusManager.isOk(it.environment.id)) {
                            logger.warn { "skipping tenant healthchecks because environment '${it.environment.reference}' is not ready" }
                            return@run
                        }

                        lockingTaskExecutor.executeWithLock<Any>({
                            if (tenantsStatusManager.needsApply(it.reference)) {
                                val client = ctx.schedulerClient
                                client.schedule(
                                    applyTask.instance(
                                        UUID.randomUUID().toString(), jacksonObjectMapper.writeValueAsString(it.reference)
                                    ),
                                    Instant.now()
                                )
                            }
                        }, LockConfiguration("${it.reference}", Instant.now().plusSeconds(600)))
                        }
                    }
                }

            fun scheduleApplyTask(
                reference: EnvironmentReference
            ) = scheduleApplyTask(scheduler, reference)

            private fun scheduleApplyTask(
                client: SchedulerClient,
                reference: EnvironmentReference
            ): UUID {
                val id = UUID.randomUUID()

                client.schedule(
                    applyTask.instance(
                        id.toString(), jacksonObjectMapper.writeValueAsString(reference)
                    ),
                    Instant.now()
                )

                return id
            }

            init {
                scheduler = Scheduler.create(dataSource, applyTask).startTasks(healthCheckTask).threads(5).build()
            }

            fun startScheduler() {
                scheduler.start()
            }
        }
        