package de.solidblocks.cli

import de.solidblocks.cli.hetzner.api.HetznerApi
import de.solidblocks.cli.hetzner.api.HetznerApiException
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HetznerApiTest {

    val hcloudToken = System.getenv("HCLOUD_TOKEN").toString()

    val api = HetznerApi(hcloudToken)

    @Test
    fun testPermissionDenied() {
        runBlocking {
            shouldThrow<RuntimeException> {
                HetznerApi("invalid").servers.list()
            }
        }
    }

    @Test
    fun testListServersPaged() {
        runBlocking { assertEquals(1, api.servers.listPaged().servers.size) }
    }

    @Test
    fun testListServers() {
        runBlocking { assertEquals(1, api.servers.list().size) }
    }

    @Test
    fun testListVolumesPaged() {
        runBlocking { assertEquals(13, api.volumes.listPaged().volumes.size) }
    }

    @Test
    fun testListVolumes() {
        runBlocking { assertEquals(13, api.volumes.list().size) }
    }

    @Test
    fun testWaitFor() {
        runBlocking {
            val volume = api.volumes.list().first()

            val result =
                api.waitFor({ api.volumes.changeProtection(volume.id, true) }, { api.volumes.action(it) })

            assertTrue(result)
        }
    }

    @Test
    fun testErrorResponse() {
        runBlocking {
            shouldThrow<HetznerApiException> {
                HetznerApi(hcloudToken).servers.create()
            }
        }
    }
}
