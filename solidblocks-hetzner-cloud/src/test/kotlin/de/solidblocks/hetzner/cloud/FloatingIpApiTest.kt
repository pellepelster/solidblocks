package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.FloatingIpCreateRequest
import de.solidblocks.hetzner.cloud.resources.FloatingIpNameFilter
import de.solidblocks.hetzner.cloud.resources.FloatingIpType
import de.solidblocks.hetzner.cloud.resources.FloatingIpUpdateRequest
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
class FloatingIpApiTest : BaseTest() {

    fun cleanup() {
        runBlocking {
            api.floatingIps.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.floatingIps.changeDeleteProtection(it.id, false)
                }
                api.floatingIps.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testFloatingIpFlow() {
        runBlocking {
            val name = "test-floating-ip"

            val created = api.floatingIps.create(
                FloatingIpCreateRequest(
                    name = name,
                    type = FloatingIpType.ipv4,
                    homeLocation = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            )
            created shouldNotBe null
            created.floatingIp.name shouldBe name
            created.floatingIp.type shouldBe FloatingIpType.ipv4
            created.floatingIp.labels shouldBe testLabels
            created.floatingIp.ip shouldNotBe null

            val byId = api.floatingIps.get(created.floatingIp.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.floatingIps.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.floatingIp.id

            api.floatingIps.list(listOf(FloatingIpNameFilter(name))) shouldHaveSize 1

            val updated = api.floatingIps.update(created.floatingIp.id, FloatingIpUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.floatingIp.labels["extra"] shouldBe "value"

            val listed = api.floatingIps.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.floatingIps.delete(created.floatingIp.id) shouldBe true

            api.floatingIps.get(created.floatingIp.id) shouldBe null
        }
    }

    @Test
    fun testFloatingIpProtectionAndDnsPtr() {
        runBlocking {
            val floatingIp = api.floatingIps.create(
                FloatingIpCreateRequest(
                    name = "test-floating-ip-protection",
                    type = FloatingIpType.ipv4,
                    homeLocation = HetznerLocation.fsn1,
                    labels = testLabels,
                ),
            ).floatingIp

            val protectAction = api.floatingIps.changeDeleteProtection(floatingIp.id, true)
            api.floatingIps.waitForAction(protectAction)

            val protected = api.floatingIps.get(floatingIp.id)
            protected!!.protection.delete shouldBe true

            val unprotectAction = api.floatingIps.changeDeleteProtection(floatingIp.id, false)
            api.floatingIps.waitForAction(unprotectAction)

            val dnsPtrAction = api.floatingIps.changeDnsPtr(floatingIp.id, floatingIp.ip, "test.example.com")
            api.floatingIps.waitForAction(dnsPtrAction)

            val clearDnsPtrAction = api.floatingIps.changeDnsPtr(floatingIp.id, floatingIp.ip, null)
            api.floatingIps.waitForAction(clearDnsPtrAction)

            api.floatingIps.delete(floatingIp.id) shouldBe true
        }
    }
}
