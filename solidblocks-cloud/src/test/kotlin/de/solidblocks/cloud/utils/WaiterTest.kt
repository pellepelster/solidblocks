package de.solidblocks.cloud.utils

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class WaiterTest {

    private val noDelay = Waiter.WaitConfig(maxIterations = 10, wait = Duration.ZERO)

    @Test
    fun `waitFor returns first non-null result`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returnsMany listOf(null, null, "found")

        val result = Waiter.waitFor(noDelay, callback)

        result shouldBe "found"
    }

    @Test
    fun `waitFor returns result immediately when first call is non-null`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returns "immediate"

        val result = Waiter.waitFor(noDelay, callback)

        result shouldBe "immediate"
    }

    @Test
    fun `waitFor returns null when callback always returns null`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returns null

        val result = Waiter.waitFor(noDelay, callback)

        result shouldBe null
    }

    @Test
    fun `waitFor returns null when maxIterations is exhausted before non-null result`(): Unit = runTest {
        val config = Waiter.WaitConfig(maxIterations = 3, wait = Duration.ZERO)
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returnsMany listOf(null, null, null, "too-late")

        val result = Waiter.waitFor(config, callback)

        result shouldBe null
    }

    @Test
    fun `waitForConsecutive returns result after required consecutive non-null results`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returnsMany listOf(null, "a", "b", "c")

        val result = Waiter.waitForConsecutive(noDelay, 3, callback)

        result shouldBe "c"
    }

    @Test
    fun `waitForConsecutive resets streak on null and still succeeds`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returnsMany listOf("a", "b", null, "x", "y", "z")

        val result = Waiter.waitForConsecutive(noDelay, 3, callback)

        result shouldBe "z"
    }

    @Test
    fun `waitForConsecutive returns null when streak is never reached within maxIterations`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        // alternates null / value, so streak never reaches 3
        coEvery { callback() } returnsMany listOf("a", "b", null, "x", "y", null, "p", "q", null, "r")

        val result = Waiter.waitForConsecutive(noDelay, 3, callback)

        result shouldBe null
    }

    @Test
    fun `waitForConsecutive returns immediately when consecutiveCount is 1`(): Unit = runTest {
        val callback = mockk<suspend () -> Int?>()
        coEvery { callback() } returnsMany listOf(null, null, 42)

        val result = Waiter.waitForConsecutive(noDelay, 1, callback)

        result shouldBe 42
    }

    @Test
    fun `waitForConsecutive returns null when callback always returns null`(): Unit = runTest {
        val callback = mockk<suspend () -> String?>()
        coEvery { callback() } returns null

        val result = Waiter.waitForConsecutive(noDelay, 2, callback)

        result shouldBe null
    }
}
