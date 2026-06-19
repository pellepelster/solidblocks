package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.api.Result

interface StringInterpolationFactory {
    val interpolationType: String

    fun validate(interpolation: String): Result<Unit>

    fun resolve(interpolation: String): Result<String>
}
