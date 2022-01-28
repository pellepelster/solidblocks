package de.solidblocks.cloud

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(TestEnvironmentExtension::class)
class SchedulerTest {

    @Test
    fun testLock(testEnvironment: TestEnvironment) {

        val executor = DefaultLockingTaskExecutor(JdbcLockProvider(testEnvironment.database.datasource))

        val result = executor.executeWithLock<String>({
            return@executeWithLock "lock-test"
        }, LockConfiguration("lockName", Instant.now().plusSeconds(10)))

            assertThat(result.result).isEqualTo("lock-test")
        }

        @Test
        @Disabled
        fun testScheduler(testEnvironment: TestEnvironment) {

            val myAdhocTask: OneTimeTask<String> = Tasks.oneTime(
                "my-typed-adhoc-task", String::class.java
            ).execute({ inst, ctx ->
                System.out.println(
                    "Executed! Custom data, Id: "
                )
            })

            val scheduler = Scheduler
                .create(testEnvironment.database.datasource, myAdhocTask)
                .threads(5).build()

            scheduler.start()

            scheduler.schedule(myAdhocTask.instance("id1", "{}"), Instant.now().plusSeconds(5))

            scheduler.schedulerState.toString()
        }
    }
    