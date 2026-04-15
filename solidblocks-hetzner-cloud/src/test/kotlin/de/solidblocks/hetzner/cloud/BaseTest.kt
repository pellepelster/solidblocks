package de.solidblocks.hetzner.cloud

open class BaseTest {
    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()
    val api = HetznerApi(hcloudToken)
    val testLabels = mapOf("blcks.de/managed-by" to "test")
}
