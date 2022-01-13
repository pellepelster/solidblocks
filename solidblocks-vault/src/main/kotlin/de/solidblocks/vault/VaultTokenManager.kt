package de.solidblocks.vault

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.Waiter
import de.solidblocks.vault.model.VaultTokenLookupResponse
import mu.KotlinLogging
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import org.springframework.web.client.HttpClientErrorException
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class VaultTokenManager(
    val address: String,
    val token: String,
    val renewAtTtl: Duration = Duration.ofMinutes(10),
    val checkInterval: Duration = Duration.ofMinutes(5)
) {

    private val logger = KotlinLogging.logger {}

    private val vaultTemplate: VaultTemplate

    private val objectMapper = jacksonObjectMapper()

    private val stop: AtomicBoolean = AtomicBoolean(false)

    var tokenInfo: VaultTokenLookupResponse? = null

    init {
        logger.info { "initializing vault token manager for address '$address'" }
        vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(address)), TokenAuthentication(token))

        thread(start = true) {
            while (!stop.get()) {
                tokenInfo = lookupToken()

                if (tokenInfo == null) {
                    logger.warn { "failed to lookup token, skipping until next try" }
                    continue
                }

                if (!tokenInfo!!.renewable) {
                    logger.error { "token is not renewable, stopping token manager" }
                    stop.set(true)
                    continue
                }

                if (tokenInfo!!.ttl < renewAtTtl) {
                    if (renew()) {
                        logger.info { "token successfully renewed" }
                    } else {
                        logger.error { "renewing token failed" }
                    }
                } else {
                    logger.info { "token is still fresh" }
                }

                Thread.sleep(checkInterval.toMillis())
            }
        }
    }

    private fun lookupToken(): VaultTokenLookupResponse? = Waiter.defaultWaiter().waitForNunNull {
        try {
            val result = vaultTemplate.read("/auth/token/lookup-self")
            objectMapper.convertValue(result.data, VaultTokenLookupResponse::class.java)
        } catch (e: HttpClientErrorException) {
            logger.error(e) { "error during token lookup" }
            null
        }
    }

    private fun renew(): Boolean = try {
        vaultTemplate.write("/auth/token/renew-self")
        true
    } catch (e: HttpClientErrorException) {
        logger.error(e) { "error renewing token" }
        false
    }
}
