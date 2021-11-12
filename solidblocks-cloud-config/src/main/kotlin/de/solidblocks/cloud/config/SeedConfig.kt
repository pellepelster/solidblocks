package de.solidblocks.cloud.config

data class SeedConfig(
    val rwUsername: String,
    val rwPassword: String,
    val roUsername: String,
    val roPassword: String,
) {
    companion object {
        const val CONFIG_RW_USERNAME = "seed_rw_username"
        const val CONFIG_RW_PASSWORD = "seed_rw_password"

        const val CONFIG_RO_USERNAME = "seed_ro_username"
        const val CONFIG_RO_PASSWORD = "seed_ro_password"
    }
}
