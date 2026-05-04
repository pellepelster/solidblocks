package de.solidblocks.cloud.utils

sealed interface Result<T>

sealed interface YamlResult<T>

data class Success<T>(val data: T) : YamlResult<T>, Result<T>

data class Error<T>(val error: String) : YamlResult<T>, Result<T>

data class YamlEmpty<T>(val message: String) : YamlResult<T>

fun Collection<Result<*>>.hasError() = this.any { it is Error<*> }

fun Collection<Result<*>>.aggregateErrorMessage() = this.filterIsInstance<Error<*>>().joinToString(", ") { it.error }

fun <T, R> Collection<Result<T>>.aggregate(block: () -> R): Result<R> = if (this.hasError()) {
    Error(this.aggregateErrorMessage())
} else {
    Success(block())
}

fun <T> Collection<Result<*>>.mapSuccess() = this.filterIsInstance<Success<T>>().map { it.data }

fun <T> catchingResult(block: () -> T): Result<T> = try {
    Success(block())
} catch (e: Exception) {
    Error<T>(e.message ?: "unknown error")
}

fun <T, R> Result<T>.map(block: (T) -> R): Result<R> = when (this) {
    is Error<T> -> Error<R>(this.error)
    is Success<T> -> Success<R>(block(data))
}
