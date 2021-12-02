package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.base.Waiter
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.core.Result
import de.solidblocks.core.logName
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.objects.general.Action
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import kotlin.reflect.KFunction

abstract class BaseHetznerProvisioner<ResourceType, RuntimeType, ApiType>(
    private val createApi: () -> ApiType,
) {

    private val logger = KotlinLogging.logger {}

    fun <RuntimeType, TargetType> Result<TargetType>.checkedApiCall(function: Result<TargetType>.(ApiType) -> TargetType): Result<TargetType> {

        if (this.failed) {
            return this
        }

        return Result(this.resource, function(createApi()))
    }

    fun <ResourceType : IResource, TargetType> checkedApiCall(
        resource: ResourceType,
        function: KFunction<Any>,
        apiCall: (ApiType) -> TargetType?
    ): Result<TargetType> {
        return checkedApiCall(resource, function.name, apiCall)
    }

    fun <ResourceType : IResource, TargetType> checkedApiCall(
        resource: ResourceType,
        action: String,
        apiCall: (ApiType) -> TargetType?
    ): Result<TargetType> {

        return Waiter.defaultWaiter().waitFor {
            try {
                val api = createApi()
                return@waitFor Result(resource, action = action, result = apiCall(api))
            } catch (e: HttpClientErrorException) {

                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked for ${resource.logName()}" }
                    return@waitFor Result(
                        resource,
                        failed = true,
                        retryable = true,
                        message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }

                    return@waitFor Result(
                        resource,
                        failed = true,
                        retryable = true,
                        message = "HTTP 429"
                    )
                }

                if (e.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn { "hetzner api request returned not found: '${e.message}'" }
                    return@waitFor Result(
                        resource,
                        failed = false,
                        retryable = false,
                        message = "HTTP 404"
                    )
                }

                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(resource, failed = true, message = e.message)
            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitFor Result(resource, failed = true, message = e.message)
            }
        }
    }

    fun <RuntimeType> waitForApiCall(
        resource: IInfrastructureResource<ResourceType, RuntimeType>,
        apiCall: (ApiType) -> Boolean
    ): Result<Boolean> {

        return Waiter.defaultWaiter().waitForSuccess {
            try {
                val result = apiCall(createApi())
                return@waitForSuccess Result(resource, result = result, failed = !result, retryable = true)
            } catch (e: HttpClientErrorException) {
                if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                    logger.warn { "hetzner api request failed (too many requests): '${e.message}'" }
                    return@waitForSuccess Result<Boolean>(
                        resource,
                        failed = true,
                        retryable = true,
                        message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.LOCKED) {
                    logger.warn { "hetzner api request returned resource is locked for ${resource.logName()}" }
                    return@waitForSuccess Result<Boolean>(
                        resource,
                        failed = true,
                        retryable = true,
                        message = e.message
                    )
                }

                if (e.statusCode == HttpStatus.NOT_FOUND) {
                    logger.warn { "hetzner api request returned not found: '${e.message}'" }
                    return@waitForSuccess Result<Boolean>(resource, failed = true, message = e.message)
                }

                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitForSuccess Result(resource, failed = true, message = e.message)
            } catch (e: Exception) {
                logger.error(e) { "hetzner api request failed '${e.message}'" }
                return@waitForSuccess Result(resource, failed = true, message = e.message)
            }
        }
    }

    fun <ResourceType : IResource> waitForActions(
        resource: ResourceType,
        function: KFunction<Any>,
        actions: List<Action>,
        call: (ApiType, Action) -> Boolean
    ): Result<Boolean> {

        val actionCalls = actions.map { action ->
            val actionCall: () -> Result<Boolean> = {
                checkedApiCall(resource, function) { api ->
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
        return callables.map {
            it.invoke()
        }.reduceResults() as Result<Boolean>
    }
}
