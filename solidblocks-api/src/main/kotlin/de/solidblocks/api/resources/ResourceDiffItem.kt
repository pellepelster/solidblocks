package de.solidblocks.api.resources

import kotlin.reflect.KProperty0

data class ResourceDiffItem(
    val name: String,
    val changed: Boolean = false,
    val triggersRecreate: Boolean = false,
    val missing: Boolean = false,
    val expectedValue: String? = null,
    val actualValue: String? = null
) {

    constructor(
        property: KProperty0<*>,
        triggersRecreate: Boolean = false,
        missing: Boolean = false,
        changed: Boolean = false,
        expectedValue: String? = null,
        actualValue: String? = null
    ) : this(property.name, triggersRecreate = triggersRecreate, missing = missing, changed = changed, expectedValue = expectedValue, actualValue = actualValue)

    fun hasChanges(): Boolean {
        return missing || changed
    }
}
