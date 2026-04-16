package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.model.toLabelSelectors
import de.solidblocks.hetzner.cloud.resources.CertificateCreateRequest
import de.solidblocks.hetzner.cloud.resources.CertificateNameFilter
import de.solidblocks.hetzner.cloud.resources.CertificateType
import de.solidblocks.hetzner.cloud.resources.CertificateType.uploaded
import de.solidblocks.hetzner.cloud.resources.CertificateTypeFilter
import de.solidblocks.hetzner.cloud.resources.CertificateUpdateRequest
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
class CertificateApiTest : BaseTest() {

    val testCert = """
        -----BEGIN CERTIFICATE-----
        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
        uiOzW4DBdO1xEydd9w==
        -----END CERTIFICATE-----
    """.trimIndent()

    val testKey = """
        -----BEGIN PRIVATE KEY-----
        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
        Uo0ZZxGY1Vi5FsEi77el4YDw0++13S0=
        -----END PRIVATE KEY-----
    """.trimIndent()

    fun cleanup() {
        runBlocking {
            api.certificates.list(labelSelectors = testLabels.toLabelSelectors()).forEach {
                api.certificates.delete(it.id)
            }
        }
    }

    @BeforeAll
    fun beforeAll() = cleanup()

    @AfterAll
    fun afterAll() = cleanup()

    @Test
    fun testCertificatesFlow() {
        runBlocking {
            val name = "test-certificate"

            val created = api.certificates.create(
                CertificateCreateRequest(
                    name = name,
                    certificate = testCert,
                    privateKey = testKey,
                    type = uploaded,
                    labels = testLabels,
                ),
            )
            created shouldNotBe null
            created.certificate.name shouldBe name
            created.certificate.type shouldBe uploaded
            created.certificate.labels shouldBe testLabels

            api.certificates.list(listOf(CertificateNameFilter(name))) shouldHaveSize 1
            api.certificates.list(listOf(CertificateNameFilter(name), CertificateTypeFilter(uploaded))) shouldHaveSize 1

            val byId = api.certificates.get(created.certificate.id)
            byId shouldNotBe null
            byId!!.name shouldBe name

            val byName = api.certificates.get(name)
            byName shouldNotBe null
            byName!!.id shouldBe created.certificate.id

            val updated = api.certificates.update(created.certificate.id, CertificateUpdateRequest(labels = testLabels + mapOf("extra" to "value")))
            updated shouldNotBe null
            updated.certificate.labels["extra"] shouldBe "value"

            val listed = api.certificates.list(labelSelectors = testLabels.toLabelSelectors())
            listed shouldHaveAtLeastSize 1

            api.certificates.delete(created.certificate.id) shouldBe true
            api.certificates.get(created.certificate.id) shouldBe null
        }
    }
}
