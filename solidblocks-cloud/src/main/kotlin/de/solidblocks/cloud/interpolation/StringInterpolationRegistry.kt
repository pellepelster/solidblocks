package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.aggregate

class StringInterpolationRegistry(val factories: List<StringInterpolationFactory>) {

    fun validate(interpolation: String): Result<Unit> {
        val error = validateInterpolatedString(interpolation)
        if (error != null) {
            return Error<Unit>(error)
        }

        val interpolations = collectInterpolatedStrings(interpolation)

        val results = interpolations.map {
            val parts = it.split(':')
            if (parts.isEmpty() || parts[0].isEmpty()) {
                Error("invalid empty interpolation")
            } else {
                val factory = factories.singleOrNull { it.interpolationType == parts[0] }
                if (factory != null) {
                    factory.validate(it.removePrefix("${parts[0]}:"))
                } else {
                    Error("invalid interpolation '$it', could not resolve type '${parts[0]}'")
                }
            }
        }

        return results.aggregate { Unit }
    }

    fun resolve(interpolation: String): Result<String> {
        val error = validateInterpolatedString(interpolation)
        if (error != null) {
            return Error<String>(error)
        }

        return replaceInterpolatedStrings(interpolation, {
            val parts = it.split(':')
            if (parts.isEmpty() || parts[0].isEmpty()) {
                Error("invalid empty interpolation")
            } else {
                val factory = factories.singleOrNull { it.interpolationType == parts[0] }

                if (factory == null) {
                    Error("invalid interpolation '$interpolation', could not resolve type '${parts[0]}'")
                } else {
                    factory.resolve(it.removePrefix("${parts[0]}:"))
                }
            }
        })
    }
}
