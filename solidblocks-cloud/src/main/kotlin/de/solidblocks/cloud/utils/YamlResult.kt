package de.solidblocks.cloud.utils

sealed interface YamlResult<T>

data class YamlEmpty<T>(val message: String) : YamlResult<T>
