package de.solidblocks.cloud.utils

import kotlinx.coroutines.delay

object Waiter {

  /*
  private val shortWaiter = Waiter(6 * 5, Duration.ofSeconds(1))

  private val defaultWaiter = Waiter(6 * 5, Duration.ofSeconds(10))

  fun defaultWaiter(): Waiter = defaultWaiter
   */

  suspend fun <T> waitForResult(callback: suspend () -> Result<T>) =
      waitForResult(DEFAULT_WAIT, callback)

  suspend fun <T> longWaitForResult(callback: suspend () -> Result<T>) =
      waitForResult(LONG_WAIT, callback)

  data class WaitConfig(val maxIterations: Int = 10, val waitTimeMs: Long = 500L)

  public val DEFAULT_WAIT = WaitConfig(30, 2000L)

  public val LONG_WAIT = WaitConfig(60, 2000L)

  suspend fun <T> waitFor(config: WaitConfig, callback: suspend () -> T?): T? {
    repeat(config.maxIterations) {
      val result = callback()
      if (result != null) return result
      delay(config.waitTimeMs)
    }
    return null
  }

  suspend fun waitForCondition(config: WaitConfig, callback: suspend () -> Boolean): Boolean {
    repeat(config.maxIterations) {
      val result = callback()
      if (result) return result
      delay(config.waitTimeMs)
    }
    return false
  }

  suspend fun <T> waitForResult(config: WaitConfig, callback: suspend () -> Result<T>): Result<T> {
    repeat(config.maxIterations) {
      val result = callback()

      when (result) {
        is Error<*> -> delay(config.waitTimeMs)
        is Success<*> -> return result
      }
    }

    return Error("error waiting for success")
  }
}
