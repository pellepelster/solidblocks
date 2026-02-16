package de.solidblocks.cloud.utils

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Waiter(maxAttempts: Int, delay: Duration) {

  private val registry: RetryRegistry =
      RetryRegistry.of(
          RetryConfig.custom<Any?>()
              .maxAttempts(maxAttempts)
              .waitDuration(delay)
              .retryOnException { it is RetryableException }
              .build(),
      )

  private val conditionRegistry: RetryRegistry =
      RetryRegistry.of(
          RetryConfig.custom<Boolean>()
              .maxAttempts(maxAttempts)
              .waitDuration(delay)
              .retryOnResult { !it }
              .retryOnException { it is RetryableException }
              .build(),
      )

  data class WaitResult<T>(val result: T, val success: Boolean)

  private val waitResultRegistry: RetryRegistry =
      RetryRegistry.of(
          RetryConfig.custom<WaitResult<Any>>()
              .maxAttempts(maxAttempts)
              .waitDuration(delay)
              .retryOnResult { !it.success }
              .retryOnException { it is RetryableException }
              .build(),
      )

  fun <T> waitFor(callable: () -> T?): T? {
    val retry = registry.retry(UUID.randomUUID().toString())
    val supplier = Retry.decorateSupplier(retry, callable)

    return supplier.get()
  }

  fun waitForCondition(callable: () -> Boolean): Boolean {
    val retry = conditionRegistry.retry(UUID.randomUUID().toString())
    val supplier = Retry.decorateSupplier(retry, callable)

    return supplier.get()
  }

  fun <T> waitForConditionWithResult(callable: () -> WaitResult<T>): WaitResult<T> {
    val retry = waitResultRegistry.retry(UUID.randomUUID().toString())
    val supplier = Retry.decorateSupplier(retry, callable)
    return supplier.get()
  }

  fun waitForCondition(countDown: Int, callable: () -> Boolean): Boolean {
    val count = AtomicInteger(countDown)

    val retry = conditionRegistry.retry(UUID.randomUUID().toString())
    val supplier =
        Retry.decorateSupplier(retry) {
          val result = callable.invoke()
          if (result) {
            count.decrementAndGet()
          }

          count.get() == 0
        }

    return supplier.get()
  }

  companion object {

    private val shortWaiter = Waiter(6 * 5, Duration.ofSeconds(1))

    private val defaultWaiter = Waiter(6 * 5, Duration.ofSeconds(10))

    fun defaultWaiter(): Waiter = defaultWaiter

    fun shortWaiter(): Waiter = shortWaiter
  }
}
