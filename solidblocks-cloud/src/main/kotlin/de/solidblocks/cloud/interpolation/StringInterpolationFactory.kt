package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Result

interface StringInterpolationFactory {
    val type: String

    fun validate(interpolation: String): Result<Unit>

    fun resolve(interpolation: String): Result<String>
}
