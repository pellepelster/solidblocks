package de.solidblocks.api.resources

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class ResourceGroupTest {

    @Test
    fun testHierarchicalResourceList() {

        val resourceGroup = ResourceGroup("group1")
        resourceGroup.addResource(TestResource("resource1", setOf(TestResource("resource2"))))

        val resourceList = resourceGroup.hierarchicalResourceList()

        assertThat(resourceList[0].name).isEqualTo("resource2")
        assertThat(resourceList[1].name).isEqualTo("resource1")
    }
}
