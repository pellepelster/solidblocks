package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.Architecture
import de.solidblocks.hetzner.cloud.resources.ImageArchitectureFilter
import de.solidblocks.hetzner.cloud.resources.ImageIncludeDeprecatedFilter
import de.solidblocks.hetzner.cloud.resources.ImageStatus
import de.solidblocks.hetzner.cloud.resources.ImageStatusFilter
import de.solidblocks.hetzner.cloud.resources.ImageType.system
import de.solidblocks.hetzner.cloud.resources.ImageTypeFilter
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageApiTest : BaseTest() {
    @Test
    fun testImagesFlow() {
        runBlocking {
            val images = api.images.list(filter = listOf(ImageTypeFilter(system)))
            images shouldHaveAtLeastSize 1

            val ubuntu = api.images.get("debian-11", filter = listOf(ImageTypeFilter(system), ImageArchitectureFilter(Architecture.x86)))
            ubuntu shouldNotBe null
            ubuntu!!.name shouldBe "debian-11"
            ubuntu.type shouldBe system

            val availableSystemImages = api.images.list(filter = listOf(ImageTypeFilter(system), ImageStatusFilter(ImageStatus.available)))
            availableSystemImages shouldHaveAtLeastSize 1
            availableSystemImages.all { it.type == system } shouldBe true

            val x86Images = api.images.list(filter = listOf(ImageTypeFilter(system), ImageArchitectureFilter(Architecture.x86)))
            x86Images shouldHaveAtLeastSize 1

            api.images.list(filter = listOf(ImageTypeFilter(system), ImageIncludeDeprecatedFilter())) shouldHaveAtLeastSize images.size

            val byId = api.images.get(ubuntu.id)
            byId shouldNotBe null
            byId!!.id shouldBe ubuntu.id
        }
    }
}
