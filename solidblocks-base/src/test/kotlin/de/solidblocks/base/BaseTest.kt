package de.solidblocks.base

import de.solidblocks.core.Result
import okhttp3.Request
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class BaseTest {

    @Test
    fun testHttpClient() {
        assertThat(defaultHttpClient().newCall(Request.Builder().url("https://heise.de").get().build()).execute().isSuccessful).isTrue
    }

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

    @Test
    fun testValidateId() {
        assertThat(validateId("aaaa")).isTrue
        assertThat(validateId("-aaaa")).isFalse
        assertThat(validateId("aaaa-")).isFalse
        assertThat(validateId("-aaaa-")).isFalse
        assertThat(validateId("1aaaa")).isFalse
        assertThat(validateId("aaaa1")).isTrue
        assertThat(validateId("1aaaa1")).isFalse
        assertThat(validateId("a-aaaa")).isTrue
        assertThat(validateId("a1aaaa")).isTrue
    }
}
