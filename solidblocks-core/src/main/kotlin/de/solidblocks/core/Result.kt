package de.solidblocks.core

import mu.KotlinLogging

fun Collection<Result<*>>.reduceResults(): Result<*> {

    if (this.isEmpty()) {
        return Result<Any>(NullResource)
    }

    return this.reduce { acc, next ->
        acc.nestedResults.add(next)
        acc
    }
}

class Result<Type>(
    var resource: IResource,
    var result: Type? = null,
    var failed: Boolean = false,
    var retryable: Boolean = false,
    var message: String? = null,
    var action: String? = null,
    val nestedResults: ArrayList<Result<*>> = ArrayList()
) {
    private val logger = KotlinLogging.logger {}

    companion object {

        fun <T> emptyResult(): Result<T> = Result(NullResource)

        fun <T> of(result: T): Result<T> = Result(NullResource, result = result)

        fun <Type1, Type2, ReturnType> onNonNullSuccess(
            resource: IResource,
            first: Result<Type1>,
            second: Result<Type2>,
            block: (result1: Type1, result2: Type2) -> ReturnType
        ): Result<ReturnType> {

            if (first.isEmptyOrFailed() || second.isEmptyOrFailed()) {
                return Result(resource, failed = true, nestedResults = arrayListOf(first, second))
            }

            return try {
                Result(
                    resource,
                    result = block(first.result!!, second.result!!),
                    nestedResults = arrayListOf(first, second)
                )
            } catch (e: RuntimeException) {
                return Result(
                    resource,
                    failed = true,
                    message = e.message,
                    nestedResults = arrayListOf(first, second)
                )
            }
        }

        fun <Type1, Type2, ReturnType> onSuccess(
            resource: IResource,
            first: Result<Type1>,
            second: Result<Type2>,
            block: (result1: Type1?, result2: Type2?) -> ReturnType
        ): Result<ReturnType> {

            if (first.failed || second.failed) {
                return Result(resource, failed = true, nestedResults = arrayListOf(first, second))
            }

            return try {
                Result(
                    resource,
                    result = block(first.result, second.result),
                    nestedResults = arrayListOf(first, second)
                )
            } catch (e: RuntimeException) {
                return Result(
                    resource,
                    failed = true,
                    message = e.message,
                    nestedResults = arrayListOf(first, second)
                )
            }
        }
    }

    fun isEmptyOrFailed(): Boolean {
        return result == null || failed
    }

    fun errorMessage(): String {
        return "result for action '${action ?: "<unknown>"}' with ${resource.logName()} was '$result', failed = $failed, error message was '${message ?: "<none>"} \n ${
        this.nestedResults.map { it.errorMessage() }.joinToString("\n")
        }"
    }

    fun <TargetType> mapNonNull(
        block: (result: Type) -> TargetType,
    ): TargetType? {

        if (failed || result == null) {
            return null
        }

        return result?.let { return block(result!!) }
    }

    fun <TargetType> mapNonNullResultNullable(
        block: (result: Type) -> TargetType?,
    ): Result<TargetType> {

        if (this.failed || result == null) {
            return this as Result<TargetType>
        }

        return try {
            Result(this.resource, block(result!!), nestedResults = arrayListOf(this))
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping non null resource result failed" }
            return Result(this.resource, failed = true, message = e.message, nestedResults = arrayListOf(this))
        }
    }

    fun <TargetType> mapNonNullResult(
        block: (result: Type) -> TargetType,
    ): Result<TargetType> {

        if (failed || result == null) {
            return this as Result<TargetType>
        }

        return try {
            Result(this.resource, block(result!!), nestedResults = arrayListOf(this))
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping non null resource result failed" }
            return Result(this.resource, failed = true, message = e.message, nestedResults = arrayListOf(this))
        }
    }

    fun <TargetType> mapNullResult(
        block: () -> TargetType,
    ): Result<*> {

        if (failed || result != null) {
            return this
        }

        return Result(this.resource, block())
    }

    fun <TargetType> mapResourceResult(
        block: (result: Type?) -> TargetType?,
    ): Result<TargetType> {

        if (this.failed) {
            return this as Result<TargetType>
        }

        return try {
            Result(this.resource, block(result), nestedResults = arrayListOf(this))
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping resource result failed" }
            return Result(this.resource, failed = true, message = e.message, nestedResults = arrayListOf(this))
        }
    }

    fun <TargetType> mapRawResourceResult(
        block: (result: Type) -> Result<TargetType>,
    ): Result<TargetType> {

        if (this.failed || this.result == null) {
            return this as Result<TargetType>
        }

        return try {
            block(result!!)
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping resource result failed" }
            return Result(this.resource, failed = true, message = e.message, nestedResults = arrayListOf(this))
        }
    }

    fun <TargetType> mapResourceResultOrElse(
        block: (result: Type) -> TargetType?,
        orElse: () -> TargetType?,
    ): Result<TargetType> {

        if (this.failed) {
            return this as Result<TargetType>
        }

        if (result == null) {
            return Result(this.resource, orElse(), nestedResults = arrayListOf(this))
        }

        return Result(this.resource, block(result!!), nestedResults = arrayListOf(this))
    }
}
