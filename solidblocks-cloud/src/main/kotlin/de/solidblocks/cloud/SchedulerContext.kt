package de.solidblocks.cloud

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
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.config.db.tables.references.SCHEDULED_TASKS
import de.solidblocks.config.db.tables.references.SHEDLOCK
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Instant
import java.util.*

class SchedulerContext(val database: SolidblocksDatabase, val repositories: RepositoriesContext, val status: StatusContext, val provisionerContext: ProvisionerContext, startSchedulers: Boolean = true) {

    private val jacksonObjectMapper = jacksonObjectMapper()

    private val logger = KotlinLogging.logger {}

    val scheduler: Scheduler

    val executor = DefaultLockingTaskExecutor(JdbcLockProvider(database.datasource))

    private val environmentApplyTask: OneTimeTask<String> = Tasks.oneTime("environments-apply", String::class.java).execute { inst, _ ->
        val reference = jacksonObjectMapper.readValue(inst.data, EnvironmentReference::class.java)
        logger.info { "executing apply for $reference" }
        executor.executeWithLock<Any>({
            provisionerContext.createEnvironmentProvisioner(reference).apply()
        }, LockConfiguration("$reference", Instant.now().plusSeconds(600)))
        }

        private val environmentHealthCheckTask = Tasks.recurring("environments-healthcheck", FixedDelay.of(CloudConstants.ENVIRONMENT_HEALTHCHECK_INTERVAL)).execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
            run {
                repositories.environments.listEnvironments().forEach {
                    val provisioner = provisionerContext.createEnvironmentProvisioner(it.reference)
                    executor.executeWithLock<Any>({
                        if (!provisioner.healthcheck()) {
                            scheduleEnvironmentApplyTask(ctx.schedulerClient, it.reference)
                        }
                    }, LockConfiguration("${it.reference}", Instant.now().plusSeconds(600)))
                    }
                }
            }

            private val tenantsApplyTask: OneTimeTask<String> = Tasks.oneTime("tenants-apply", String::class.java).execute { inst, _ ->
                val reference = jacksonObjectMapper.readValue(inst.data, TenantReference::class.java)
                executor.executeWithLock<Any>({
                    provisionerContext.createTenantProvisioner(reference).apply()
                }, LockConfiguration("$reference", Instant.now().plusSeconds(600)))
                }

                private val tenantsHealthCheckTask = Tasks.recurring("tenants-healthcheck", FixedDelay.ofSeconds(15)).execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
                    run {
                        repositories.tenants.listTenants().forEach {
                            if (!status.environments.isOk(it.environment.id)) {
                                logger.warn { "skipping tenant healthchecks because environment '${it.environment.reference}' is not ready" }
                                return@run
                            }

                            executor.executeWithLock<Any>({
                                if (status.tenants.needsApply(it.reference)) {
                                    scheduleTenantApplyTask(it.reference)
                                }
                            }, LockConfiguration("${it.reference}", Instant.now().plusSeconds(600)))
                            }
                        }
                    }

                    init {

                        val dsl = database.dsl

                        logger.info { "cleaning leftover locks from '${SHEDLOCK.name}'" }
                        dsl.deleteFrom(SHEDLOCK).execute()

                        logger.info { "cleaning leftover scheduled tasks from '$SCHEDULED_TASKS'" }
                        dsl.deleteFrom(SCHEDULED_TASKS).execute()

                        repositories.status.cleanupEphemeralData()
                        scheduler = Scheduler.create(database.datasource, environmentApplyTask, tenantsApplyTask)
                            .enableImmediateExecution()
                            .startTasks(tenantsHealthCheckTask, environmentHealthCheckTask)
                            .threads(5).build()

                        if (startSchedulers) {
                            scheduler.start()
                        }
                    }

                    fun scheduleEnvironmentApplyTask(reference: EnvironmentReference) = scheduleEnvironmentApplyTask(scheduler, reference)

                    private fun scheduleEnvironmentApplyTask(client: SchedulerClient, reference: EnvironmentReference): UUID {
                        val id = UUID.randomUUID()

                        client.schedule(environmentApplyTask.instance(id.toString(), jacksonObjectMapper.writeValueAsString(reference)), Instant.now())

                        return id
                    }

                    fun scheduleTenantApplyTask(reference: TenantReference) = scheduleTenantApplyTask(scheduler, reference)

                    private fun scheduleTenantApplyTask(client: SchedulerClient, reference: TenantReference): UUID {
                        val id = UUID.randomUUID()

                        client.schedule(tenantsApplyTask.instance(id.toString(), jacksonObjectMapper.writeValueAsString(reference)), Instant.now())

                        return id
                    }
                }
                