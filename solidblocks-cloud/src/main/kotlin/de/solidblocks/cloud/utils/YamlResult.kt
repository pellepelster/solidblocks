package de.solidblocks.cloud.utils

sealed interface Result<T>

sealed interface YamlResult<T>

data class Success<T>(val data: T) : YamlResult<T>, Result<T>

data class Error<T>(val error: String) : YamlResult<T>, Result<T>

data class YamlEmpty<T>(val message: String) : YamlResult<T>

fun Collection<YamlResult<*>>.hasError() = this.any { it is Error<*> }

fun Collection<YamlResult<*>>.aggregateErrors() =
    this.filterIsInstance<Error<*>>().joinToString(", ") { it.error }

fun <T> Collection<YamlResult<*>>.mapSuccess() = this.filterIsInstance<Success<T>>().map { it.data }
