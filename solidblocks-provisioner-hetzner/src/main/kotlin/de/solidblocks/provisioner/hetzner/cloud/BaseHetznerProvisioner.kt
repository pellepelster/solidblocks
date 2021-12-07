package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.base.Waiter
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.Result
import de.solidblocks.core.logName
import me.tomsdevsn.hetznercloud.objects.general.Action
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import kotlin.reflect.KFunction

abstract class BaseHetznerProvisioner<ResourceType, RuntimeType, ApiType>(val hetznerCloudAPI: ApiType) {

    private val logger = KotlinLogging.logger {}

    fun <RuntimeType, TargetType> Result<TargetType>.checkedApiCall(function: Result<TargetType>.(ApiType) -> TargetType): Result<TargetType> {

        if (this.failed) {
            return this
        }

        return Result(function(hetznerCloudAPI))
    }

    fun <TargetType> checkedApiCall(
            function: KFunction<Any>,
            apiCall: (ApiType) -> TargetType?
    ): Result<TargetType> {
        return checkedApiCall(function.name, apiCall)
    }

    fun <TargetType> checkedApiCall(
            action: String,
            apiCall: (ApiType) -> TargetType?
    ): Result<TargetType> {
        return Waiter.defaultWaiter().waitFor {
            try {
                return@waitFor Result(action = action, result = apiCall(hetznerCloudAPI))
            } catch (e: HttpClientErrorException) {
                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked" }
                    return@waitFor Result(
                            failed = true,
                            retryable = true,
                            message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }

                    return@waitFor Result(
                        failed = true,
                        retryable = true,
                        message = "HTTP 429"
                    )
                }

                if (e.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn { "hetzner api request returned not found: '${e.message}'" }
                    return@waitFor Result(
                        failed = false,
                        retryable = false,
                        message = "HTTP 404"
                    )
                }

                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(failed = true, message = e.message)
            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(failed = true, message = e.message)
            }
        }
    }

    fun <RuntimeType> waitForApiCall(
        resource: IInfrastructureResource<ResourceType, RuntimeType>,
        apiCall: (ApiType) -> Boolean
    ): Result<Boolean> {
        return Waiter.defaultWaiter().waitFor {
            try {
                val result = apiCall(hetznerCloudAPI)
                return@waitFor Result(result = result, failed = !result, retryable = true)
            } catch (e: HttpClientErrorException) {
                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }
                    return@waitFor Result<Boolean>(
                        failed = true,
                        retryable = true,
                        message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked for ${resource.logName()}" }
                    return@waitFor Result<Boolean>(
                        failed = true,
                        retryable = true,
                        message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn { "hetzner api request returned not found: '${e.message}'" }
                    return@waitFor Result<Boolean>(failed = true, message = e.message)
                }

                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(failed = true, message = e.message)
            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(failed = true, message = e.message)
            }
        }
    }

    fun waitForActions(
            function: KFunction<Any>,
            actions: List<Action>,
            call: (ApiType, Action) -> Boolean
    ): Result<Boolean> {

        val actionCalls = actions.map { action ->
            val actionCall: () -> Result<Boolean> = {
                checkedApiCall(function) { api ->
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

        if (results.any { it.isEmptyOrFailed() }) {
            return Result(failed = true)
        }

        return Result()
    }
}
