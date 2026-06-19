package de.solidblocks.cloud.utils

sealed interface YamlResult<T>

data class YamlSuccess<T>(val data: T) : YamlResult<T>

data class YamlError<T>(val error: String, val cause: Throwable? = null) : YamlResult<T>

data class YamlEmpty<T>(val message: String) : YamlResult<T>
