package de.solidblocks.core

import mu.KotlinLogging

class Result<Type>(
    var result: Type? = null,
    var failed: Boolean = false,
    var retryable: Boolean = false,
    var message: String? = null,
    var action: String? = null,
) {
    private val logger = KotlinLogging.logger {}

    companion object {

        fun <T> emptyResult(): Result<T> = Result()

        fun <T> failedResult(): Result<T> = Result(failed = true)

        fun <T> resultOf(result: T): Result<T> = Result(result = result)

        fun <Type1, Type2, ReturnType> onSuccess(
                first: Result<Type1>,
                second: Result<Type2>,
                block: (result1: Type1?, result2: Type2?) -> ReturnType
        ): Result<ReturnType> {

            if (first.failed || second.failed) {
                return Result(failed = true)
            }

            return try {
                Result(
                        result = block(first.result, second.result))
            } catch (e: RuntimeException) {
                return Result(
                        failed = true,
                        message = e.message)
            }
        }
    }

    fun isEmptyOrFailed(): Boolean {
        return result == null || failed
    }

    fun success(): Boolean {
        return !failed
    }

    fun isEmpty(): Boolean {
        return result == null || failed
    }

    fun errorMessage(): String {
        return "result for action '${action ?: "<unknown>"}' was '${result.toString()}', failed = $failed, error message was '${message ?: "<none>"}"
    }

    fun <TargetType> mapNonNullResult(
        block: (result: Type) -> TargetType,
    ): Result<TargetType> {

        if (failed || result == null) {
            return this as Result<TargetType>
        }

        return try {
            Result(block(result!!))
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping non null resource result failed" }
            return Result(failed = true, message = e.message)
        }
    }

    fun <TargetType> mapResourceResult(
        block: (result: Type?) -> TargetType?,
    ): Result<TargetType> {

        if (this.failed) {
            return this as Result<TargetType>
        }

        return try {
            Result(block(result))
        } catch (e: RuntimeException) {
            logger.error(e) { "mapping resource result failed" }
            return Result(failed = true, message = e.message)
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
            return Result(orElse())
        }

        return Result(block(result!!))
    }

    fun mapSuccessNonNullBoolean(
            block: (result: Type) -> Boolean,
    ): Boolean {

        if (this.failed) {
            return false
        }

        return block(result!!)
    }

}
