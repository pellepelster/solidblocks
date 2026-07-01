package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.OtherLookup
import de.solidblocks.cloud.api.OtherResource
import de.solidblocks.cloud.api.TestLookup
import de.solidblocks.cloud.api.TestResource
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffItem
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.tainted
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourceGroupTest {

    @Test
    fun `hierarchical resource list orders dependencies before dependents`() {
        val parent = TestResource("parent")
        val child = TestResource("child", dependsOn = setOf(parent))

        val ordered = ResourceGroup("g", listOf(child, parent)).hierarchicalResourceList()

        ordered.indexOf(parent) shouldBeLessThan ordered.indexOf(child)
    }

    @Test
    fun `hierarchical resource list resolves dependencies expressed as lookups`() {
        val target = TestResource("a")
        val dependent = OtherResource("b", dependsOn = setOf(TestLookup("a")))

        val ordered = ResourceGroup("g", listOf(dependent, target)).hierarchicalResourceList()

        ordered.indexOf(target) shouldBeLessThan ordered.indexOf(dependent)
    }

    @Test
    fun `hierarchical resource list ignores lookups that resolve to no resource`() {
        val dependent = OtherResource("b", dependsOn = setOf(TestLookup("does-not-exist")))

        val ordered = ResourceGroup("g", listOf(dependent)).hierarchicalResourceList()

        ordered.contains(dependent) shouldBe true
    }

    @Test
    fun `allChangedOrMissingDiffs keeps only missing and changed diffs`() {
        val missingResource = TestResource("missing")
        val upToDateResource = TestResource("upToDate")
        val changedResource = TestResource("changed")
        val taintedResource = TestResource("tainted")

        val diffs = mapOf(
            ResourceGroup("g", listOf(missingResource, upToDateResource, changedResource, taintedResource)) to
                listOf(
                    ResourceDiff(missingResource, missing),
                    ResourceDiff(upToDateResource, up_to_date),
                    ResourceDiff(changedResource, has_changes, changes = listOf(ResourceDiffItem("x", changed = true))),
                    ResourceDiff(taintedResource, tainted),
                ),
        )

        diffs.allDiffs() shouldHaveSize 4
        diffs.allChangedOrMissingResources() shouldContainExactly listOf(missingResource, changedResource)
    }

    @Test
    fun `hierarchical resource list throws on a dependency cycle`() {
        val a = TestResource("a", dependsOn = setOf(OtherLookup("b")))
        val b = OtherResource("b", dependsOn = setOf(TestLookup("a")))

        shouldThrow<RuntimeException> {
            ResourceGroup("g", listOf(a, b)).hierarchicalResourceList()
        }
    }

    @Test
    fun `log text describes the resource group`() {
        ResourceGroup("common").logText() shouldBe "resource group 'common'"
    }
}
