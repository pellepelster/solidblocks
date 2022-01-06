package de.solidblocks.base

import de.solidblocks.core.Result
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.vavr.control.Try
import java.time.Duration
import java.util.*

class Waiter(maxAttempts: Int, delay: Duration) {

    private val resultConfig = RetryConfig.custom<Result<Any>>()
        .maxAttempts(maxAttempts)
        .waitDuration(delay)
        .retryOnResult { it.retryable }
        .build()
    private val resultRegistry: RetryRegistry = RetryRegistry.of(resultConfig)

    private val config = RetryConfig.custom<Boolean>()
        .maxAttempts(maxAttempts)
        .waitDuration(delay)
        .retryOnResult { it }
        .build()
    private val registry: RetryRegistry = RetryRegistry.of(config)

    private val nunNullConfig = RetryConfig.custom<Any?>()
        .maxAttempts(maxAttempts)
        .waitDuration(delay)
        .retryOnResult { it == null }
        .build()
    private val nonNullRegistry: RetryRegistry = RetryRegistry.of(nunNullConfig)

    fun <T> waitForResult(callable: () -> Result<T>): Result<T> {

        val retry = resultRegistry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return Try.ofSupplier(supplier).get()
    }

    fun <T> waitForNunNull(callable: () -> T): T {

        val retry = nonNullRegistry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return Try.ofSupplier(supplier).get()
    }

    fun waitFor(callable: () -> Boolean): Boolean {

        val retry = registry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return Try.ofSupplier(supplier).get()
    }

    companion object {
        private val SHORT_WAITER = Waiter(6 * 5, Duration.ofSeconds(1))

        private val DEFAULT_WAITER = Waiter(6 * 5, Duration.ofSeconds(10))

        fun defaultWaiter(): Waiter {
            return DEFAULT_WAITER
        }

        fun shortWaiter(): Waiter {
            return SHORT_WAITER
        }
    }
}
