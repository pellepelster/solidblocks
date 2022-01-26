package de.solidblocks.base

import de.solidblocks.base.resources.ResourcePermission
import de.solidblocks.base.resources.parsePermissions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResourcePermissionTest {

    @Test
    fun testParseInvalid() {
        assertThat(ResourcePermission.parse("srn:cloud1:env1:tenant1:service1:xxx:yyy")).isNull()
        assertThat(ResourcePermission.parse("xxx:cloud1:env1:tenant1:service1")).isNull()
    }

    @Test
    fun testSinglePermission() {
        val cloudPermission = ResourcePermission.parse("srn:cloud1:env1:tenant1")
        assertThat(cloudPermission).isNotNull
        assertThat(cloudPermission?.cloud?.wildcard).isFalse
        assertThat(cloudPermission?.cloud?.cloud).isEqualTo("cloud1")

        val cloudWildcardPermission = ResourcePermission.parse("srn::env1:tenant1")
        assertThat(cloudWildcardPermission).isNotNull
        assertThat(cloudWildcardPermission?.cloud?.wildcard).isTrue
        assertThat(cloudWildcardPermission?.cloud?.cloud).isNull()

        val environmentPermission = ResourcePermission.parse("srn:cloud1:env1:tenant1")
        assertThat(environmentPermission).isNotNull
        assertThat(environmentPermission?.environment?.wildcard).isFalse
        assertThat(environmentPermission?.environment?.environment).isEqualTo("env1")

        val environmentWildcardPermission = ResourcePermission.parse("srn:cloud1::tenant1")
        assertThat(environmentWildcardPermission).isNotNull
        assertThat(environmentWildcardPermission?.environment?.wildcard).isTrue
        assertThat(environmentWildcardPermission?.environment?.environment).isNull()

        val tenantPermission = ResourcePermission.parse("srn:cloud1:env1:tenant1")
        assertThat(tenantPermission).isNotNull
        assertThat(tenantPermission?.tenant?.wildcard).isFalse
        assertThat(tenantPermission?.tenant?.tenant).isEqualTo("tenant1")

        val tenantWildcardPermission = ResourcePermission.parse("srn:cloud1::")
        assertThat(tenantWildcardPermission).isNotNull
        assertThat(tenantWildcardPermission?.tenant?.wildcard).isTrue
        assertThat(tenantWildcardPermission?.tenant?.tenant).isNull()
    }

    @Test
    fun testInvalidMultiplePermissions() {
        assertThat(listOf("srn:cloud1:env1:tenant1", "xxx").parsePermissions().isCloudWildcard).isFalse
    }

    @Test
    fun testMultiplePermissions() {
        assertThat(listOf("srn::env1:tenant1", "srn:cloud2:env1:tenant1").parsePermissions().isCloudWildcard).isTrue
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env1:tenant1").parsePermissions().isCloudWildcard).isFalse

        assertThat(listOf("srn::env1:tenant1", "srn:cloud2:env1:tenant1").parsePermissions().clouds).isEqualTo(listOf("cloud2"))
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env1:tenant1").parsePermissions().clouds).isEqualTo(listOf("cloud1", "cloud2"))

        assertThat(listOf("srn:cloud1::tenant1", "srn:cloud2:env2:tenant1").parsePermissions().isEnvironmentWildcard).isTrue
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env2:tenant1").parsePermissions().isEnvironmentWildcard).isFalse

        assertThat(listOf("srn:cloud1::tenant1", "srn:cloud2:env2:tenant1").parsePermissions().environments).isEqualTo(listOf("env2"))
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env2:tenant1").parsePermissions().environments).isEqualTo(listOf("env1", "env2"))

        assertThat(listOf("srn:cloud1::", "srn:cloud2:env2:tenant1").parsePermissions().isTenantWildcard).isTrue
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env2:tenant2").parsePermissions().isTenantWildcard).isFalse

        assertThat(listOf("srn:cloud1::", "srn:cloud2:env2:tenant2").parsePermissions().tenants).isEqualTo(listOf("tenant2"))
        assertThat(listOf("srn:cloud1:env1:tenant1", "srn:cloud2:env2:tenant2").parsePermissions().tenants).isEqualTo(listOf("tenant1", "tenant2"))
    }
}
