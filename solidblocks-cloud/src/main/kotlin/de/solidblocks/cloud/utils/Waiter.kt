package de.solidblocks.cloud.utils

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class WaitConfig(val maxIterations: Int = 10, val wait: Duration = 2.seconds)

public val SHORT_WAIT = WaitConfig(10, 1.seconds)

public val DEFAULT_WAIT = WaitConfig(30, 1.seconds)

public val LONG_WAIT = WaitConfig(90, 5.seconds)

suspend fun <T> WaitConfig.waitFor(callback: suspend () -> T?) = this.waitForConsecutive(1, callback)

suspend fun <T> WaitConfig.waitForConsecutive(consecutiveCount: Int = 1, callback: suspend () -> T?): T? {
    var streak = 0
    var lastResult: T? = null
    repeat(this.maxIterations) {
        val result = callback()
        if (result != null) {
            streak++
            lastResult = result
            if (streak >= consecutiveCount) {
                return lastResult
            }
        } else {
            streak = 0
            lastResult = null
        }
        delay(this.wait)
    }
    return null
}

suspend fun WaitConfig.waitForCondition(callback: suspend () -> Boolean): Boolean {
    repeat(this.maxIterations) {
        val result = callback()
        if (result) {
            return true
        }
        delay(this.wait)
    }
    return false
}

suspend fun <T> WaitConfig.waitForResult(callback: suspend () -> Result<T>): Result<T> {
    repeat(this.maxIterations) {
        when (val result = callback()) {
            is Error<*> -> delay(this.wait)
            is Success<*> -> return result
        }
    }

    return Error("error waiting for success")
}
