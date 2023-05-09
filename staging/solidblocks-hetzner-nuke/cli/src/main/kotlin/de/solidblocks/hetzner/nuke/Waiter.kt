package de.solidblocks.hetzner.nuke

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import java.time.Duration
import java.util.*

class Waiter(maxAttempts: Int, delay: Duration) {

    private val config = RetryConfig.custom<Boolean>()
        .maxAttempts(maxAttempts)
        .waitDuration(delay)
        .retryOnResult { !it }
        .build()
    private val registry: RetryRegistry = RetryRegistry.of(config)

    fun waitFor(callable: () -> Boolean): Boolean {

        val retry = registry.retry(UUID.randomUUID().toString())
        val supplier = Retry.decorateSupplier(retry, callable)

        return supplier.get()
    }

    companion object {
        private val DEFAULT_WAITER = Waiter(6 * 5, Duration.ofSeconds(5))

        fun defaultWaiter(): Waiter {
            return DEFAULT_WAITER
        }
    }
}
