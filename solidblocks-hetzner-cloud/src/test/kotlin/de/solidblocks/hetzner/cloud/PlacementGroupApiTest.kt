package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.PlacementGroupCreateRequest
import de.solidblocks.hetzner.cloud.resources.PlacementGroupNameFilter
import de.solidblocks.hetzner.cloud.resources.PlacementGroupType
import de.solidblocks.hetzner.cloud.resources.PlacementGroupTypeFilter
import de.solidblocks.hetzner.cloud.resources.PlacementGroupUpdateRequest
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlacementGroupApiTest {
    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)
    val testLabels = mapOf("blcks.de/managed-by" to "test")

    fun cleanup() {
        runBlocking {
            api.placementGroups.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.placementGroups.delete(it.id)
            }
        }
    }

    @BeforeAll fun beforeAll() = cleanup()

    @AfterAll fun afterAll() = cleanup()

    @Test
    fun testPlacementGroupsFlow() {
        runBlocking {
            val name = "test-placement-group"

            val created = api.placementGroups.create(PlacementGroupCreateRequest(name, PlacementGroupType.spread, testLabels))
            created shouldNotBe null
            created.placementGroup.name shouldBe name
            created.placementGroup.type shouldBe PlacementGroupType.spread
            created.placementGroup.labels shouldBe testLabels

            val byId = api.placementGroups.get(created.placementGroup.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.placementGroups.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.placementGroup.id

            api.placementGroups.list(listOf(PlacementGroupNameFilter(name))) shouldHaveSize 1

            val byType = api.placementGroups.list(listOf(PlacementGroupTypeFilter(PlacementGroupType.spread)), labelSelectors = testLabels.toLabelSelectors())
            byType shouldHaveSize 1
            byType.first().type shouldBe PlacementGroupType.spread

            val updated = api.placementGroups.update(created.placementGroup.id, PlacementGroupUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated!!.placementGroup.labels["extra"] shouldBe "value"

            val listed = api.placementGroups.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.placementGroups.delete(created.placementGroup.id) shouldBe true

            api.placementGroups.get(created.placementGroup.id) shouldBe null
        }
    }
}
