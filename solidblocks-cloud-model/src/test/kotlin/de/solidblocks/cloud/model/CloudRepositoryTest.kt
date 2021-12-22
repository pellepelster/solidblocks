package de.solidblocks.cloud.model

import de.solidblocks.cloud.model.model.CloudConfigValue
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class CloudRepositoryTest {

    @Test
    fun testCreateCloud(solidblocksDatabase: SolidblocksDatabase) {

        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)

        assertThat(cloudRepository.hasCloud("cloud1")).isFalse

        val cloud = cloudRepository.createCloud("cloud1", "domain1", listOf(CloudConfigValue("name1", "value1")))
        assertThat(cloud).isNotNull

        assertThat(cloudRepository.hasCloud("cloud1")).isTrue

        val configWithoutEnv = cloudRepository.getCloud("cloud1")
        assertThat(configWithoutEnv.name).isEqualTo("cloud1")
        assertThat(configWithoutEnv.rootDomain).isEqualTo("domain1")
    }

    @Test()
    fun testDoesNotAllowDuplicateCloudNames(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        cloudRepository.createCloud("cloud2", "domain1")

        Assertions.assertThrows(DataAccessException::class.java) {
            cloudRepository.createCloud("cloud2", "domain2")
        }
    }
}
