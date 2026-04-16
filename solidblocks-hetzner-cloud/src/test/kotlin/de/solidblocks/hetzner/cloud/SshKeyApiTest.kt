package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.SSHKeysCreateRequest
import de.solidblocks.hetzner.cloud.resources.SSHKeysUpdateRequest
import de.solidblocks.hetzner.cloud.resources.SshKeyNameFilter
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
class SshKeyApiTest : BaseTest() {

    fun cleanup() {
        runBlocking {
            api.sshKeys.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.sshKeys.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testSSHFlow() {
        runBlocking {
            val name = "test-ssh-key"
            val publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBxP0oGDT9p8mf6n6J4X5N3aTT6I/RxqxTJ7gbN4lVWi test@test"

            val created = api.sshKeys.create(SSHKeysCreateRequest(name, publicKey, testLabels))
            created.sshKey shouldNotBe null
            created.sshKey.name shouldBe name
            created.sshKey.labels shouldBe testLabels

            val byId = api.sshKeys.get(created.sshKey.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.sshKeys.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.sshKey.id

            api.sshKeys.list(listOf(SshKeyNameFilter(name))) shouldHaveSize 1

            val updated = api.sshKeys.update(created.sshKey.id, SSHKeysUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.sshKey.labels["extra"] shouldBe "value"

            val listed = api.sshKeys.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.sshKeys.delete(created.sshKey.id) shouldBe true

            api.sshKeys.get(created.sshKey.id) shouldBe null
        }
    }
}
