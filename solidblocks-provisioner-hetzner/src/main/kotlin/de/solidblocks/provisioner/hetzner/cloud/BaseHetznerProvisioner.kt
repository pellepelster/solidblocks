package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.base.Waiter
import de.solidblocks.core.Result
import me.tomsdevsn.hetznercloud.objects.general.Action
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

abstract class BaseHetznerProvisioner<ResourceType, RuntimeType, ApiType>(val hetznerCloudAPI: ApiType) {

    private val logger = KotlinLogging.logger {}

    fun <RuntimeType, TargetType> Result<TargetType>.checkedApiCall(function: Result<TargetType>.(ApiType) -> TargetType): Result<TargetType> {

        if (this.failed) {
            return this
        }

        return Result(function(hetznerCloudAPI))
    }

    fun <TargetType> checkedApiCall(
            apiCall: (ApiType) -> TargetType?
    ): Result<TargetType> {
        return Waiter.defaultWaiter().waitForResult {
            try {
                return@waitForResult Result(result = apiCall(hetznerCloudAPI))
            } catch (e: HttpClientErrorException) {
                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked" }
                    return@waitForResult Result(
                            failed = true,
                            retryable = true,
                            message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }

                    return@waitForResult Result(
                        failed = true,
                        retryable = true,
                        message = "HTTP 429"
                    )
                }

                if (e.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn { "hetzner api request returned not found: '${e.message}'" }
                    return@waitForResult Result(
                        failed = false,
                        retryable = false,
                        message = "HTTP 404"
                    )
                }

                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitForResult Result(failed = true, message = e.message)
            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitForResult Result(failed = true, message = e.message)
            }
        }
    }

    fun waitForRetryableApiCall(
        apiCall: (ApiType) -> Boolean
    ): Boolean {
        return Waiter.defaultWaiter().waitFor {
            try {
                return@waitFor !apiCall(hetznerCloudAPI)
            } catch (e: HttpClientErrorException) {
                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }
                    return@waitFor true
                }

                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked" }
                    return@waitFor true
                }

            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
            }

            return@waitFor false
        }
    }

    fun waitForActions(
        actions: List<Action>,
        call: (ApiType, Action) -> Boolean
    ): Result<Boolean> {
        val actionCalls = actions.map { action ->
            val actionCall: () -> Result<Boolean> = {
                checkedApiCall { api ->
                    call(api, action)
                }
            }
            actionCall
        }

        return waitForActions(actionCalls)
    }

    fun waitForActions(
        callables: List<() -> Result<Boolean>>
    ): Result<Boolean> {
        val results = callables.map {
            it.invoke()
        }

        if (results.any { it.failed }) {
            return Result(failed = true)
        }

        return Result(failed = false)
    }
}
