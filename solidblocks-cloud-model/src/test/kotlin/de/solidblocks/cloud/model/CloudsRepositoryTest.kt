package de.solidblocks.cloud.model

import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.resources.parsePermissions
import de.solidblocks.cloud.model.entities.CloudConfigValue
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class CloudsRepositoryTest {

    @Test
    fun testPermissions(database: SolidblocksDatabase) {

        val cloudsRepository = CloudsRepository(database.dsl)

        val cloud1 = UUID.randomUUID().toString()
        val cloud2 = UUID.randomUUID().toString()

        cloudsRepository.createCloud(cloud1, cloud1)
        cloudsRepository.createCloud(cloud2, cloud2)

        assertThat(cloudsRepository.hasCloud(cloud1)).isTrue

        assertThat(cloudsRepository.getCloud(cloud1)).isNotNull
        assertThat(cloudsRepository.getCloud(cloud1, "srn:::".parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloud(cloud1, "srn:$cloud1::".parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloud(cloud1, "srn:$cloud1:env1:".parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloud(cloud1, "srn:$cloud1:env1:tenant1".parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloud(cloud1, "srn:$cloud2::".parsePermissions())).isNull()
        assertThat(cloudsRepository.getCloud(cloud1, listOf("srn:$cloud1::", "srn:$cloud2::").parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloud(cloud2, listOf("srn:$cloud1::", "srn:$cloud2::").parsePermissions())).isNotNull

        assertThat(cloudsRepository.getCloudByRootDomain(cloud1)).isNotNull
        assertThat(cloudsRepository.getCloudByRootDomain(cloud1, "srn:$cloud1::".parsePermissions())).isNotNull
        assertThat(cloudsRepository.getCloudByRootDomain(cloud1, "srn:$cloud2::".parsePermissions())).isNull()
        assertThat(cloudsRepository.getCloud(CloudReference(cloud1))).isNotNull
        assertThat(cloudsRepository.getCloud(CloudReference(cloud1), "srn:cloud2::".parsePermissions())).isNull()
    }

    @Test
    fun testCreateCloud(database: SolidblocksDatabase) {

        val cloudsRepository = CloudsRepository(database.dsl)

        assertThat(cloudsRepository.hasCloud("cloud1")).isFalse

        val cloud = cloudsRepository.createCloud(
            "cloud1",
            "domain1",
            listOf(CloudConfigValue("name1", "value1"))
        )
        assertThat(cloud).isNotNull

        assertThat(cloudsRepository.hasCloud("cloud1")).isTrue

        val getCloud = cloudsRepository.getCloud("cloud1")!!
        assertThat(getCloud.name).isEqualTo("cloud1")
        assertThat(getCloud.rootDomain).isEqualTo("domain1")
    }

    @Test
    fun testDoesNotAllowDuplicateCloudNames(database: SolidblocksDatabase) {
        val cloudsRepository = CloudsRepository(database.dsl)
        cloudsRepository.createCloud("cloud2", "domain1")

        Assertions.assertThrows(DataAccessException::class.java) {
            cloudsRepository.createCloud("cloud2", "domain2")
        }
    }
}
