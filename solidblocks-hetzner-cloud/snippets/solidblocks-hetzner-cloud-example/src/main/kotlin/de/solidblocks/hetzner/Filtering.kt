package de.solidblocks.hetzner

import de.solidblocks.hetzner.cloud.HetznerApi
import de.solidblocks.hetzner.cloud.model.LabelSelectorValue.Equals
import de.solidblocks.hetzner.cloud.resources.ImageType
import de.solidblocks.hetzner.cloud.resources.ImageTypeFilter
import kotlinx.coroutines.runBlocking

fun Filtering() {
    runBlocking {
        val api = HetznerApi(System.getenv("HCLOUD_TOKEN"))

        // list all images
        api.images.list()

        // filter by image type
        api.images.list(listOf(ImageTypeFilter(ImageType.app)))

        // filter by label
        api.servers.list(labelSelectors = mapOf("label1" to Equals("foo-bar")))
    }
}