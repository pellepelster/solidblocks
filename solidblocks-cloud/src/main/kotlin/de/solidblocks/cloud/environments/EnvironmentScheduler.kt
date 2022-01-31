package de.solidblocks.cloud.environments

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.cloud.ProvisionerContext
import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Instant
import javax.sql.DataSource

class EnvironmentScheduler(
    val dataSource: DataSource,
    val environmentsRepository: EnvironmentsRepository,
    val provisionerContext: ProvisionerContext
) {

    val executor = DefaultLockingTaskExecutor(JdbcLockProvider(dataSource))

    private val logger = KotlinLogging.logger {}

    fun startScheduler() {

        val applyTask = Tasks.recurring("my-hourly-task-1", FixedDelay.ofSeconds(10))
            .execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
                run {
                    val client = ctx.schedulerClient
                    logger.info { "checking all environment for changes" }

                    environmentsRepository.listEnvironments().forEach {
                        logger.info { "checking environment '${it.reference}' for changes" }

                        val provisioner = provisionerContext.createEnvironmentProvisioner(it.reference)
                        provisioner.apply()
                    }
                }
            }

        val myAdhocTask: OneTimeTask<String> = Tasks.oneTime(
            "my-typed-adhoc-task", String::class.java
        ).execute({ inst, ctx ->

            System.out.println(
                "Executed! Custom data, Id: "
            )
        })

        val scheduler = Scheduler
            .create(dataSource, myAdhocTask)
            .enableImmediateExecution()
            .startTasks(applyTask)
            .threads(5).build()

        scheduler.start()

        scheduler.schedule(myAdhocTask.instance("id1", "{}"), Instant.now().plusSeconds(5))
    }
}
