package de.solidblocks.base

import de.solidblocks.core.Result
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class UtilsTest {

    @Test
    fun testGenerateSshKey() {
        val result = Utils.generateSshKey("test")
        assertThat(result.first).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(result.first).endsWith("-----END OPENSSH PRIVATE KEY-----\n")
        assertThat(result.second).startsWith("ssh-ed25519")
    }

    @Test
    fun testWaiter() {
        val failedResult = Waiter.DEFAULT_WAITER.waitFor { Result<Any>(NullResource, failed = true) }
        assertThat(failedResult.failed).isTrue

        val successResult = Waiter.DEFAULT_WAITER.waitFor { Result<Any>(NullResource, failed = false) }
        assertThat(successResult.failed).isFalse

        val atomic = AtomicInteger(0)
        val retryResult = Waiter.SHORT_WAITER.waitFor {
            if (atomic.incrementAndGet() < 3) {
                return@waitFor Result<Any>(NullResource, retryable = true)
            } else {
                return@waitFor Result<Any>(NullResource, failed = false)
            }
        }
        assertThat(retryResult.failed).isFalse
    }
}
