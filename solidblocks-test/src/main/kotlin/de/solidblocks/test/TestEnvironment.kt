package de.solidblocks.test

import de.solidblocks.base.reference.UserReference
import de.solidblocks.cloud.ManagersContext
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.RepositoriesContext

class TestEnvironment {

    val testContext: TestApplicationContext

    val reference = UserReference("cloud1", "environment1", "tenant1", "user1")

    val repositories: RepositoriesContext
        get() = testContext.repositories

    val managers: ManagersContext
        get() = testContext.managers

    val database: SolidblocksDatabase
        get() = testContext.database

    init {
        testContext = TestApplicationContext(TestConstants.TEST_DB_JDBC_URL(), development = true)
    }
}
