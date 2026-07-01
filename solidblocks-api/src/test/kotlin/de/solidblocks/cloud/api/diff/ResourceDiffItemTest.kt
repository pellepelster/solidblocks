package de.solidblocks.cloud.api.diff

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourceDiffItemTest {

    private class Holder {
        val myField: String = "v"
    }

    @Test
    fun `log text renders an expected versus actual comparison`() {
        ResourceDiffItem("field", expectedValue = "e", actualValue = "a").logText() shouldBe "field should be 'e' but was 'a'"
    }

    @Test
    fun `log text renders empty placeholder for null values`() {
        ResourceDiffItem("field", expectedValue = "e", actualValue = null).logText() shouldBe "field should be 'e' but was '<empty>'"
    }

    @Test
    fun `log text prefers the missing state over a value comparison`() {
        ResourceDiffItem("field", missing = true, expectedValue = "e", actualValue = null).logText() shouldBe "field is missing"
    }

    @Test
    fun `log text renders missing changed recreate and unchanged states`() {
        ResourceDiffItem("field", missing = true).logText() shouldBe "field is missing"
        ResourceDiffItem("field", changed = true).logText() shouldBe "field changed"
        ResourceDiffItem("field", triggersRecreate = true).logText() shouldBe "field changed and requires recreate"
        ResourceDiffItem("field").logText() shouldBe "field has no changes"
    }

    @Test
    fun `hasChanges is true when missing changed or triggers recreate`() {
        ResourceDiffItem("field", missing = true).hasChanges() shouldBe true
        ResourceDiffItem("field", changed = true).hasChanges() shouldBe true
        ResourceDiffItem("field", triggersRecreate = true).hasChanges() shouldBe true
        ResourceDiffItem("field").hasChanges() shouldBe false
    }

    @Test
    fun `property constructor derives the name from the property`() {
        val item = ResourceDiffItem(Holder()::myField, changed = true)

        item.name shouldBe "myField"
        item.changed shouldBe true
    }
}
