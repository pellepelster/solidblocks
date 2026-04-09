package de.solidblocks.cloud.utils

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Waiter {

    suspend fun <T> waitForResult(callback: suspend () -> Result<T>) = waitForResult(DEFAULT_WAIT, callback)

    suspend fun <T> longWaitForResult(callback: suspend () -> Result<T>) = waitForResult(LONG_WAIT, callback)

    data class WaitConfig(val maxIterations: Int = 10, val wait: Duration = 2.seconds)

    public val DEFAULT_WAIT = WaitConfig(30, 1.seconds)

    public val LONG_WAIT = WaitConfig(90, 5.seconds)

    suspend fun <T> waitFor(config: WaitConfig, callback: suspend () -> T?): T? {
        repeat(config.maxIterations) {
            val result = callback()
            if (result != null) {
                return result
            }
            delay(config.wait)
        }
        return null
    }

    suspend fun waitForCondition(config: WaitConfig, callback: suspend () -> Boolean): Boolean {
        repeat(config.maxIterations) {
            val result = callback()
            if (result) {
                return true
            }
            delay(config.wait)
        }
        return false
    }

    suspend fun <T> waitForResult(config: WaitConfig, callback: suspend () -> Result<T>): Result<T> {
        repeat(config.maxIterations) {
            when (val result = callback()) {
                is Error<*> -> delay(config.wait)
                is Success<*> -> return result
            }
        }

        return Error("error waiting for success")
    }
}
