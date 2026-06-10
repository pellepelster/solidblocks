package de.solidblocks.cloud.providers

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

/**
 * Declares how many providers of a given category may be configured. Which category a provider
 * belongs to is declared by its [ProviderRegistration.category].
 *
 * Constant names are lowercased on purpose so [name] renders directly in error messages.
 */
enum class ProviderCategory(val min: Int, val max: Int?) {
    cloud(0, 1),
    ssh_key(1, 1),
    backup(0, 1),
    secret(0, 1),
    github(0, 1),
    ;

    /** Human-readable description of the allowed count for error messages. */
    fun expectation(): String = when {
        max == null -> "at least $min"
        min == max -> "exactly $min"
        min == 0 -> "at most $max"
        else -> "between $min and $max"
    }
}

/**
 * Validates that the number of [configured] providers in each category is within the bounds
 * declared by [ProviderCategory]. Categories are checked in declaration order so the reported
 * violation is deterministic.
 */
fun List<ProviderRegistration<*, *, *>>.validateCardinality(configured: List<ProviderConfiguration>): Result<Unit> {
    ProviderCategory.entries.forEach { category ->
        val registrations = this.filter { it.category == category }
        val configTypes = registrations.map { it.supportedConfiguration }.toSet()
        val count = configured.count { it::class in configTypes }

        val violates = count < category.min || (category.max != null && count > category.max)
        if (violates) {
            val types = registrations.joinToString(", ") { "'${it.type}'" }
            return Error(
                "invalid number of '${category.name}' providers, found $count but expected ${category.expectation()}. available types are: $types",
            )
        }
    }

    return Success(Unit)
}
