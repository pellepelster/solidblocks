package de.solidblocks.cloud.api.diff

import de.solidblocks.cloud.api.TestResource
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.tainted
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class ResourceDiffTest {

    @Test
    fun `needsRecreate is true for a tainted resource that requires recreate`() {
        val resource = TestResource("a", taintRequiresRecreate = true)
        ResourceDiff(resource, tainted).needsRecreate() shouldBe true
    }

    @Test
    fun `needsRecreate is false for a tainted resource that does not require recreate`() {
        val resource = TestResource("a", taintRequiresRecreate = false)
        ResourceDiff(resource, tainted).needsRecreate() shouldBe false
    }

    @Test
    fun `needsRecreate is true when any change triggers recreate`() {
        val diff = ResourceDiff(
            TestResource("a"),
            ResourceDiffStatus.has_changes,
            changes = listOf(ResourceDiffItem("x", triggersRecreate = true)),
        )
        diff.needsRecreate() shouldBe true
    }

    @Test
    fun `hasChanges reflects whether any change item has changes`() {
        ResourceDiff(TestResource("a"), up_to_date).hasChanges() shouldBe false
        ResourceDiff(
            TestResource("a"),
            ResourceDiffStatus.has_changes,
            changes = listOf(ResourceDiffItem("x", changed = true)),
        ).hasChanges() shouldBe true
    }

    @Test
    fun `toString describes the diff status`() {
        ResourceDiff(TestResource("a"), missing).toString() shouldBe "testresource 'a' is missing"
        ResourceDiff(TestResource("a"), up_to_date).toString() shouldBe "testresource 'a' is up to date"
        ResourceDiff(
            TestResource("a"),
            ResourceDiffStatus.has_changes,
            changes = listOf(ResourceDiffItem("x", changed = true)),
        ).toString() shouldContain "has changes ="
    }

    @Test
    fun `list log text groups changed and missing resources`() {
        val diffs = listOf(
            ResourceDiff(
                TestResource("changed"),
                ResourceDiffStatus.has_changes,
                changes = listOf(ResourceDiffItem("field", changed = true)),
            ),
            ResourceDiff(TestResource("gone"), missing),
        )

        val text = diffs.logText()

        text shouldContain "changed resources:"
        text shouldContain "testresource 'changed'"
        text shouldContain "missing resources:"
        text shouldContain "testresource 'gone'"
    }
}
