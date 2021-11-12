package de.solidblocks.base

import de.solidblocks.core.Result
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.vavr.control.Try
import java.time.Duration
import java.util.UUID

class Waiter(maxAttempts: Int, delay: Duration) {

    private val config: RetryConfig = RetryConfig.custom<Result<Any>>()
        .maxAttempts(maxAttempts)
        .waitDuration(delay)
        .retryOnResult { it.failed && it.retryable }
        .build()

    private val registry: RetryRegistry = RetryRegistry.of(config)

    fun <T> waitFor(callable: () -> Result<T>): Result<T> {

        val retry = registry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return Try.ofSupplier(supplier).get()
    }

    fun waitForSuccess(callable: () -> Result<Boolean>): Result<Boolean> {

        val retry = registry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return Try.ofSupplier(supplier).get()
    }

    companion object {
        private val DEFAULT_WAITER = Waiter(6 * 5, Duration.ofSeconds(10))

        fun defaultWaiter(): Waiter {
            return DEFAULT_WAITER
        }
    }
}
