package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.PrimaryIpAssigneeType
import de.solidblocks.hetzner.cloud.resources.PrimaryIpCreateRequest
import de.solidblocks.hetzner.cloud.resources.PrimaryIpIpFilter
import de.solidblocks.hetzner.cloud.resources.PrimaryIpNameFilter
import de.solidblocks.hetzner.cloud.resources.PrimaryIpType
import de.solidblocks.hetzner.cloud.resources.PrimaryIpUpdateRequest
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
class PrimaryIpApiTest : BaseTest() {
    fun cleanup() {
        runBlocking {
            api.primaryIps.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                if (it.protection.delete) {
                    api.primaryIps.changeDeleteProtection(it.id, false)
                    api.primaryIps.waitForAction(api.primaryIps.changeDeleteProtection(it.id, false))
                }
                api.primaryIps.delete(it.id)
            }
        }
    }

    @BeforeAll fun beforeAll() = cleanup()

    @AfterAll fun afterAll() = cleanup()

    @Test
    fun testPrimaryIpFlow() {
        runBlocking {
            val name = "test-primary-ip"

            val created = api.primaryIps.create(
                PrimaryIpCreateRequest(
                    name = name,
                    type = PrimaryIpType.ipv4,
                    assigneeType = PrimaryIpAssigneeType.server,
                    datacenter = "fsn1-dc14",
                    labels = testLabels,
                ),
            )
            created shouldNotBe null
            created.primaryIp.name shouldBe name
            created.primaryIp.type shouldBe PrimaryIpType.ipv4
            created.primaryIp.labels shouldBe testLabels

            val byId = api.primaryIps.get(created.primaryIp.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.primaryIps.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.primaryIp.id

            api.primaryIps.list(listOf(PrimaryIpNameFilter(name))) shouldHaveSize 1
            api.primaryIps.list(listOf(PrimaryIpIpFilter(created.primaryIp.ip))) shouldHaveSize 1

            val updated = api.primaryIps.update(created.primaryIp.id, PrimaryIpUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.primaryIp.labels["extra"] shouldBe "value"

            val listed = api.primaryIps.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.primaryIps.delete(created.primaryIp.id) shouldBe true

            api.primaryIps.get(created.primaryIp.id) shouldBe null
        }
    }

    @Test
    fun testPrimaryIpProtectionAssignAndDnsPtr() {
        runBlocking {
            val primaryIp = api.primaryIps.create(
                PrimaryIpCreateRequest(
                    name = "test-primary-ip-protection",
                    type = PrimaryIpType.ipv4,
                    assigneeType = PrimaryIpAssigneeType.server,
                    datacenter = "fsn1-dc14",
                    labels = testLabels,
                ),
            ).primaryIp

            val protectAction = api.primaryIps.changeDeleteProtection(primaryIp.id, true)
            api.primaryIps.waitForAction(protectAction)

            val protected = api.primaryIps.get(primaryIp.id)
            protected!!.protection.delete shouldBe true

            val unprotectAction = api.primaryIps.changeDeleteProtection(primaryIp.id, false)
            api.primaryIps.waitForAction(unprotectAction)

            val dnsPtrAction = api.primaryIps.changeDnsPtr(primaryIp.id, primaryIp.ip, "test.example.com")
            api.primaryIps.waitForAction(dnsPtrAction)

            val clearDnsPtrAction = api.primaryIps.changeDnsPtr(primaryIp.id, primaryIp.ip, null)
            api.primaryIps.waitForAction(clearDnsPtrAction)

            api.primaryIps.delete(primaryIp.id) shouldBe true
        }
    }
}
