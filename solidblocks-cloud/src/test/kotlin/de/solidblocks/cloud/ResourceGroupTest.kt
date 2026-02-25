package de.solidblocks.cloud

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourceGroupTest {

    @Test
    fun testLookupCheck() {


        val mock1 = MockResource1("name1")
        val mock1Lookup = MockResource1Lookup("name1")

        //val mock1LookupTest : MockResource1Lookup = mock1

        val mock2 = MockResource1("name1")
        val mock2Lookup = MockResource1Lookup("name1")

        mock1Lookup.isLookupFor(mock1) shouldBe true
        mock1Lookup.isLookupFor(mock2) shouldBe false
    }
}