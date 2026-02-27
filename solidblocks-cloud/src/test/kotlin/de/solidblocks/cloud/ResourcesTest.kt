package de.solidblocks.cloud

import de.solidblocks.cloud.api.hierarchicalResourceList
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourcesTest {

    @Test
    fun testLookupTypes() {
        val mock1 = MockResource1("name1")
        val mock1Lookup = MockResource1Lookup("name1")

        val mock2 = MockResource2("name1")
        val mock2Lookup = MockResource1Lookup("name1")

        mock1Lookup.isLookupFor(mock1) shouldBe true
        mock1Lookup.isLookupFor(mock2) shouldBe false
    }

    @Test
    fun testHierarchicalResourceList() {
        val mock1 = MockResource1("name1")
        val mock1Lookup1 = MockResource1Lookup("name1")

        val mock2 = MockResource2("name1", setOf(mock1))
        val mock2Lookup = MockResource1Lookup("name1")

        val mock1Lookup2 = MockResource1Lookup("name1")
        val mock3 = MockResource3("name1", setOf(mock1Lookup2))

        (mock1Lookup1 == mock1Lookup2) shouldBe true

        val hierarchicalResourceList = listOf(mock3, mock1Lookup1, mock2, mock2Lookup, mock1).hierarchicalResourceList()

        hierarchicalResourceList shouldHaveSize 4
        hierarchicalResourceList[0]::class shouldBe mock1::class
        hierarchicalResourceList[1]::class shouldBe mock2::class
        hierarchicalResourceList[2]::class shouldBe mock1Lookup1::class
        hierarchicalResourceList[3]::class shouldBe mock3::class
    }
}