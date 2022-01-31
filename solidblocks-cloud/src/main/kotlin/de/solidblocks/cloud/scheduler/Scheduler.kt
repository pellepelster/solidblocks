package de.solidblocks.cloud.scheduler

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider
import java.time.Instant
import javax.sql.DataSource

class Scheduler(dataSource: DataSource) {

    val executor = DefaultLockingTaskExecutor(JdbcLockProvider(dataSource))

    fun testScheduler(dataSource: DataSource) {

        val myAdhocTask: OneTimeTask<String> = Tasks.oneTime(
            "my-typed-adhoc-task", String::class.java
        ).execute({ inst, ctx ->
            System.out.println(
                "Executed! Custom data, Id: "
            )
        })

        val scheduler = Scheduler
            .create(dataSource, myAdhocTask)
            .threads(5).build()

        scheduler.start()

        scheduler.schedule(myAdhocTask.instance("id1", "{}"), Instant.now().plusSeconds(5))

        scheduler.schedulerState.toString()
    }
}
