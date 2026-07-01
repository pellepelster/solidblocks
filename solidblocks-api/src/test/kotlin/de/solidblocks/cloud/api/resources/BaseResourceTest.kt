package de.solidblocks.cloud.api.resources

import de.solidblocks.cloud.api.TestLookup
import de.solidblocks.cloud.api.TestResource
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BaseResourceTest {

    @Test
    fun `log text derives a name from the class and strips the lookup suffix`() {
        TestResource("a").logText() shouldBe "testresource 'a'"
        TestLookup("a").logText() shouldBe "test 'a'"
    }

    @Test
    fun `log text falls back to the superclass name for anonymous classes`() {
        val resource = object : BaseInfrastructureResource<Any>("a", emptySet()) {
            override val lookupType = TestLookup::class

            override fun asLookup() = TestLookup(name)
        }

        resource.logText() shouldBe "baseinfrastructureresource 'a'"
    }

    @Test
    fun `recursive depends on collects transitive dependencies`() {
        val a = TestResource("a")
        val b = TestResource("b", dependsOn = setOf(a))
        val c = TestResource("c", dependsOn = setOf(b))

        c.recursiveDependsOn() shouldContainExactlyInAnyOrder setOf(a, b)
    }

    @Test
    fun `recursive depends on deduplicates diamond dependencies`() {
        val base = TestResource("base")
        val left = TestResource("left", dependsOn = setOf(base))
        val right = TestResource("right", dependsOn = setOf(base))
        val top = TestResource("top", dependsOn = setOf(left, right))

        top.recursiveDependsOn() shouldContainExactlyInAnyOrder setOf(left, right, base)
    }

    @Test
    fun `recursive depends on terminates on dependency cycles`() {
        val dependenciesOfA = mutableSetOf<BaseResource>()
        val dependenciesOfB = mutableSetOf<BaseResource>()
        val a = object : BaseResource("a", dependenciesOfA) {}
        val b = object : BaseResource("b", dependenciesOfB) {}
        dependenciesOfA.add(b)
        dependenciesOfB.add(a)

        a.recursiveDependsOn() shouldContainExactlyInAnyOrder setOf(a, b)
    }

    @Test
    fun `is lookup for matches on lookup type and name`() {
        val resource = TestResource("a")

        TestLookup("a").isLookupFor(resource) shouldBe true
        TestLookup("b").isLookupFor(resource) shouldBe false
    }
}
