package de.solidblocks.cloud.interpolation

data class SolidblocksResourceName(val type: SolidblocksResourceType) {

    companion object {
        fun parse(value: String): SolidblocksResourceName {
            val parts = value.split(':')

            TODO()
        }
    }
}

enum class SolidblocksResourceType {
    secret,
}
