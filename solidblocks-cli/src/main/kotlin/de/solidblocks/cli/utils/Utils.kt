package de.solidblocks.cli.utils

sealed interface Result<T>

data class Success<T>(val data: T) : Result<T>

data class Error<T>(val error: String) : Result<T>

class Empty<T>(val message: String) : Result<T>

fun Collection<Result<*>>.hasError() = this.any { it is Error<*> }

fun Collection<Result<*>>.aggregateErrors() =
    this.filterIsInstance<Error<*>>().joinToString { it.error }

fun <T> Collection<Result<*>>.mapSuccess() = this.filterIsInstance<Success<T>>().map { it.data }

public fun List<String>.indentWithYamlObjectMarker() =
    this.withIndex().map {
      if (it.index == 0) {
        "- ${it.value}"
      } else {
        "  ${it.value}"
      }
    }

