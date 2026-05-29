package de.solidblocks.cloud.utils

sealed interface Result<T>

data class Success<T>(val data: T) : YamlResult<T>, Result<T>

data class Error<T>(val error: String, val cause: Throwable? = null) : YamlResult<T>, Result<T>

fun Collection<Result<*>>.hasError() = this.any { it is Error<*> }

fun Collection<Result<*>>.aggregateErrorMessage() = this.filterIsInstance<Error<*>>().joinToString(", ") { it.error }

fun <T, R> Collection<Result<T>>.aggregate(block: () -> R): Result<R> = if (this.hasError()) {
    Error(this.aggregateErrorMessage())
} else {
    Success(block())
}

fun <T> Collection<Result<T>>.mapSuccess() = this.filterIsInstance<Success<T>>().map { it.data }

fun <T> catchingResult(block: () -> T): Result<T> = try {
    Success(block())
} catch (e: Exception) {
    Error<T>(e.message ?: "unknown error", e)
}

fun <T, R> Result<T>.map(block: (T) -> R): Result<R> = when (this) {
    is Error<T> -> Error<R>(this.error, this.cause)
    is Success<T> -> Success<R>(block(data))
}

inline fun <T, R> Result<T>.flatMap(f: (T) -> Result<R>): Result<R> = when (this) {
    is Success<T> -> f(data)
    is Error<T> -> Error(error, cause)
}

inline fun <T, R> Result<T>.fold(onError: (Error<T>) -> R, onSuccess: (T) -> R): R = when (this) {
    is Success<T> -> onSuccess(data)
    is Error<T> -> onError(this)
}

inline fun <T> Result<T>.getOrElse(f: (Error<T>) -> T): T = when (this) {
    is Success<T> -> data
    is Error<T> -> f(this)
}

inline fun <T> Result<T>.onError(f: (Error<T>) -> Unit): Result<T> = also { if (it is Error<T>) f(it) }

fun <T> Error<*>.retype(): Error<T> = Error(error, cause)

inline fun <T> result(block: ResultScope.() -> T): Result<T> = try {
    Success(ResultScope.block())
} catch (e: ResultScope.Raise) {
    @Suppress("UNCHECKED_CAST")
    e.error as Result<T>
}

object ResultScope {
    fun <T> Result<T>.bind(): T = when (this) {
        is Success<T> -> data
        is Error<T> -> throw Raise(this)
    }

    class Raise(val error: Error<*>) : RuntimeException(null, null, false, false)
}
