package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class EnvironmentVariableInterpolationFactory() : StringInterpolationFactory {
    override val interpolationType = "env"

    override fun validate(interpolation: String): Result<Unit> {
        val parts = interpolation.split(':', limit = 2)

        return if (parts[0].isEmpty()) {
            Error("invalid empty environment variable reference")
        } else {
            Success(Unit)
        }
    }

    override fun resolve(interpolation: String): Result<String> {
        val parts = interpolation.split(':', limit = 2)
        val varName = parts[0]
        val value = System.getenv(varName)

        return when {
            varName.isEmpty() -> Error("invalid empty environment variable reference")
            value != null -> Success(value)
            parts.size == 2 -> Success(parts[1])
            else -> Error("environment variable '$varName' is not set")
        }
    }
}
