package de.solidblocks.base

import de.solidblocks.core.Result
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class BaseTest {

    @Test
    fun testWaiter() {
        val failedResult = Waiter.defaultWaiter().waitForResult { Result<Any>(NullResource, failed = true) }
        assertThat(failedResult.failed).isTrue

        val successResult = Waiter.defaultWaiter().waitForResult { Result<Any>(NullResource, failed = false) }
        assertThat(successResult.failed).isFalse

        val atomic = AtomicInteger(0)
        val retryResult = Waiter.shortWaiter().waitForResult {
            if (atomic.incrementAndGet() < 3) {
                return@waitForResult Result<Any>(NullResource, retryable = true)
            } else {
                return@waitForResult Result<Any>(NullResource, failed = false)
            }
        }
        assertThat(retryResult.failed).isFalse
    }
}
