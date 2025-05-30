package de.solidblocks.cli

import de.solidblocks.cli.hetzner.HetznerApi
import de.solidblocks.cli.hetzner.HetznerApiException
import de.solidblocks.cli.utils.getenv
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HetznerApiTest {

    val hcloudToken = getenv("HCLOUD_TOKEN").toString()

    val api = HetznerApi(hcloudToken)

    fun testPermissionDenied() {
        runBlocking {
            assertFailsWith<RuntimeException> { HetznerApi("invalid").servers.list() }
        }
    }

    fun testListServersPaged() {
        runBlocking {
            assertEquals(1, api.servers.listPaged().servers.size)
        }
    }

    fun testListServers() {
        runBlocking {
            assertEquals(1, api.servers.list().size)
        }
    }

    fun testListVolumesPaged() {
        runBlocking {
            assertEquals(21, api.volumes.listPaged().volumes.size)
        }
    }

    fun testListVolumes() {
        runBlocking {
            assertEquals(21, api.volumes.list().size)
        }
    }

    fun testWaitFor() {
        runBlocking {
            val volume = api.volumes.list().first()

            val result = api.waitFor({
                api.volumes.changeProtection(volume.id, true)
            }, {
                api.volumes.action(it)
            })

            assertTrue(result)
        }
    }

    fun testErrorResponse() {
        runBlocking {
            assertFailsWith<HetznerApiException> {
                HetznerApi(hcloudToken).servers.create()
            }
        }
    }
}