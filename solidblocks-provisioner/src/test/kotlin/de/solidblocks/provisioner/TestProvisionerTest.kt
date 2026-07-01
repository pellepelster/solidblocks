package de.solidblocks.provisioner

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffItem
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.*
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.endpoint.EndpointProtocol
import de.solidblocks.cloud.api.resources.ResourceGroup
import de.solidblocks.provisioner.mock.ApplyBehaviour.error_on_apply
import de.solidblocks.provisioner.mock.ApplyBehaviour.throw_exception_on_apply
import de.solidblocks.provisioner.mock.DiffBehaviour.change_no_recreate
import de.solidblocks.provisioner.mock.DiffBehaviour.duplicate_on_diff
import de.solidblocks.provisioner.mock.DiffBehaviour.error_on_diff
import de.solidblocks.provisioner.mock.DiffBehaviour.force_recreate_change
import de.solidblocks.provisioner.mock.DiffBehaviour.parent_missing_on_diff
import de.solidblocks.provisioner.mock.DiffBehaviour.throw_exception_on_diff
import de.solidblocks.provisioner.mock.DiffBehaviour.unknown_on_diff
import de.solidblocks.provisioner.mock.DiffBehaviour.up_to_date_or_missing
import de.solidblocks.provisioner.mock.EndpointResource
import de.solidblocks.provisioner.mock.EndpointResourceProvisioner
import de.solidblocks.provisioner.mock.Resource1
import de.solidblocks.provisioner.mock.Resource1Provisioner
import de.solidblocks.provisioner.mock.Resource2
import de.solidblocks.provisioner.mock.Resource2Lookup
import de.solidblocks.provisioner.mock.Resource2Provisioner
import de.solidblocks.provisioner.mock.TestApplyContext
import de.solidblocks.provisioner.mock.TestDiffContext
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import java.util.UUID

class TestProvisionerTest {

    @Test
    fun `resource is missing if parent is missing`() {
        val resource1Provisioner = Resource1Provisioner()
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(
                        resourceProvisioners = listOf(resource1Provisioner, Resource2Provisioner()),
                    ),
                )

            val resource1 = Resource1("test1", up_to_date_or_missing)
            val resource2 = Resource2("test2", up_to_date_or_missing, setOf(resource1))

            assertSoftly(resource1Provisioner.diff(resource1, TestDiffContext()).shouldBeInstanceOf<Success<ResourceDiff>>()) {
                it.data.status shouldBe missing
            }

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource1, resource2))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].resource.name shouldBe "test1"
                it.values.first()[0].status shouldBe missing
                it.values.first()[1].resource.name shouldBe "test2"
                it.values.first()[1].status shouldBe missing
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            assertSoftly(
                provisioner
                    .diff(
                        listOf(
                            ResourceGroup("common", listOf(resource1, resource2)),
                        ),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>(),
            ) {
                it.data.entries shouldHaveSize 1
                it.data.values.first() shouldHaveSize 2
                it.data.values.first()[0].status shouldBe up_to_date
            }
        }
    }

    @Test
    fun `error on diff aborts the plan`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("error_on_diff", error_on_diff)

            provisioner
                .diff(
                    listOf(ResourceGroup("common", listOf(resource))),
                    { false },
                    TEST_LOG_CONTEXT,
                )
                .shouldBeTypeOf<Error<Map<ResourceGroup, List<ResourceDiff>>>>()
        }
    }

    @Test
    fun `unexpected exception during diff aborts plan`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("throw_exception_on_diff", throw_exception_on_diff)

            provisioner
                .diff(
                    listOf(ResourceGroup("common", listOf(resource))),
                    { false },
                    TEST_LOG_CONTEXT,
                )
                .shouldBeTypeOf<Error<Map<ResourceGroup, List<ResourceDiff>>>>()
        }
    }

    @Test
    fun `missing resources are created`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)
            val resource2ForceRecreateChange = Resource2(resourceName, force_recreate_change)

            provisioner
                .apply(
                    listOf(resource2),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(
                            ResourceGroup("common", listOf(resource2ForceRecreateChange)),
                        ),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].resource.name shouldBe resourceName
                it.values.first()[0].status shouldBe has_changes
                it.values.first()[0].needsRecreate() shouldBe true
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe true
            resource2Provisioner.isApplied(resourceName) shouldBe true
        }
    }

    @Test
    fun `unknown diff status does not abort the plan`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("unknown_on_diff", unknown_on_diff)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].status shouldBe unknown
            }
        }
    }

    @Test
    fun `duplicate diff status aborts the plan`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("duplicate_on_diff", duplicate_on_diff)

            assertSoftly(
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Error<Map<ResourceGroup, List<ResourceDiff>>>>(),
            ) {
                it.error shouldBe "duplicate error for ${resource.logText()}"
            }
        }
    }

    @Test
    fun `taint callback taints resource and apply recreates it`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)

            provisioner
                .apply(
                    listOf(resource2),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].resource.name shouldBe resourceName
                it.values.first()[0].status shouldBe tainted
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe false
            resource2Provisioner.applyCount(resourceName) shouldBe 2
        }
    }

    @Test
    fun `tainted resource is destroyed before apply if taint requires recreate`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing, taintRequiresRecreate = true)

            provisioner
                .apply(
                    listOf(resource2),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].status shouldBe tainted
                it.values.first()[0].needsRecreate() shouldBe true
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe true
            resource2Provisioner.applyCount(resourceName) shouldBe 2
        }
    }

    @Test
    fun `resource that is not taintable is reported as up to date`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing, taintable = false)

            provisioner
                .apply(
                    listOf(resource2),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].status shouldBe up_to_date
            }
        }
    }

    @Test
    fun `tainted resource taints dependent resources`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val parentName = UUID.randomUUID().toString()
            val childName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val parent = Resource2(parentName, up_to_date_or_missing)
            val child = Resource2(childName, up_to_date_or_missing, setOf(parent))

            provisioner
                .apply(
                    listOf(parent, child),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(parent, child))),
                        { it.name == parentName },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].resource.name shouldBe parentName
                it.values.first()[0].status shouldBe tainted
                it.values.first()[1].resource.name shouldBe childName
                it.values.first()[1].status shouldBe tainted
            }
        }
    }

    @Test
    fun `duplicate diff aborts apply`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("duplicate", up_to_date_or_missing)
            val diffs = mapOf(
                ResourceGroup("common", listOf(resource)) to
                    listOf(ResourceDiff(resource, duplicate, duplicateErrorMessage = "duplicate error")),
            )

            assertSoftly(
                provisioner
                    .apply(diffs, TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "duplicate error"
            }
        }
    }

    @Test
    fun `duplicate diff aborts apply before destroying recreate resources`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            provisioner
                .apply(
                    listOf(Resource2(resourceName, up_to_date_or_missing)),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val recreateResource = Resource2(resourceName, force_recreate_change)
            val duplicateResource = Resource2("duplicate", up_to_date_or_missing)
            val diffs = mapOf(
                ResourceGroup("common", listOf(recreateResource, duplicateResource)) to
                    listOf(
                        ResourceDiff(recreateResource, has_changes, changes = listOf(ResourceDiffItem("x", triggersRecreate = true))),
                        ResourceDiff(duplicateResource, duplicate, duplicateErrorMessage = "duplicate error"),
                    ),
            )

            assertSoftly(
                provisioner
                    .apply(diffs, TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "duplicate error"
            }

            resource2Provisioner.isDestroyed(resourceName) shouldBe false
        }
    }

    @Test
    fun `failed destroy aborts apply`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            provisioner
                .apply(
                    listOf(Resource2(resourceName, up_to_date_or_missing)),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(Resource2(resourceName, force_recreate_change)))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            resource2Provisioner.destroyResult = false

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Error<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe true
            resource2Provisioner.applyCount(resourceName) shouldBe 1
        }
    }

    @Test
    fun `destroy is skipped for resources that do not exist`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(Resource2(resourceName, force_recreate_change)))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe false
            resource2Provisioner.isApplied(resourceName) shouldBe true
        }
    }

    @Test
    fun `error during apply aborts rollout`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("error_on_apply", up_to_date_or_missing, applyBehaviour = error_on_apply)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(
                provisioner
                    .apply(diffs, TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "apply error for ${resource.logText()}"
            }
        }
    }

    @Test
    fun `unexpected exception during apply aborts rollout`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("throw_exception_on_apply", up_to_date_or_missing, applyBehaviour = throw_exception_on_apply)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Error<Unit>>()
        }
    }

    @Test
    fun `error during apply of resource list is reported as error`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("error_on_apply", up_to_date_or_missing, applyBehaviour = error_on_apply)

            assertSoftly(
                provisioner
                    .apply(listOf(resource), TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "failed to apply 1 resource(s): ${resource.logText()}"
            }
        }
    }

    @Test
    fun `exception during apply of resource list is reported as error`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val resource = Resource2("throw_exception_on_apply", up_to_date_or_missing, applyBehaviour = throw_exception_on_apply)

            assertSoftly(
                provisioner
                    .apply(listOf(resource), TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "failed to apply 1 resource(s): ${resource.logText()}"
            }
        }
    }

    @Test
    fun `up to date resources are not re-applied`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)

            provisioner
                .apply(
                    listOf(resource2),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].status shouldBe up_to_date
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.applyCount(resourceName) shouldBe 1
        }
    }

    @Test
    fun `changed resources are applied in place without destroy`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(resource2Provisioner)),
                )

            provisioner
                .apply(
                    listOf(Resource2(resourceName, up_to_date_or_missing)),
                    TestApplyContext(),
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(Resource2(resourceName, change_no_recreate)))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].status shouldBe has_changes
                it.values.first()[0].needsRecreate() shouldBe false
            }

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe false
            resource2Provisioner.applyCount(resourceName) shouldBe 2
        }
    }

    @Test
    fun `parent_missing status marks dependent resources as missing`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(Resource2Provisioner())),
                )

            val parent = Resource2("parent", parent_missing_on_diff)
            val child = Resource2("child", up_to_date_or_missing, setOf(parent))

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(parent, child))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].resource.name shouldBe "parent"
                it.values.first()[0].status shouldBe parent_missing
                it.values.first()[1].resource.name shouldBe "child"
                it.values.first()[1].status shouldBe missing
            }
        }
    }

    @Test
    fun `resource depending on a missing lookup is reported as missing`() {
        runBlocking {
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(
                        resourceProvisioners = listOf(Resource1Provisioner(), Resource2Provisioner()),
                    ),
                )

            val parent = Resource2("parent", up_to_date_or_missing)
            val child = Resource1("child", up_to_date_or_missing, setOf(Resource2Lookup("parent")))

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(parent, child))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs.values.first()) {
                it.first { d -> d.resource.name == "parent" }.status shouldBe missing
                it.first { d -> d.resource.name == "child" }.status shouldBe missing
            }
        }
    }

    @Test
    fun `endpoints are waited for after apply`() {
        runBlocking {
            val endpointProvisioner = EndpointResourceProvisioner()
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(endpointProvisioner)),
                )

            val endpoint = Endpoint("server", "10.0.0.1", 22, EndpointProtocol.ssh)
            val resource = EndpointResource("server", listOf(endpoint))

            val waitedFor = mutableListOf<Endpoint>()
            provisioner.waitForEndpointResult = {
                waitedFor.add(it)
                Success(Unit)
            }

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            provisioner
                .apply(diffs, TestApplyContext())
                .shouldBeTypeOf<Success<Unit>>()

            waitedFor shouldContainExactly listOf(endpoint)
        }
    }

    @Test
    fun `failed endpoint wait aborts rollout`() {
        runBlocking {
            val endpointProvisioner = EndpointResourceProvisioner()
            val provisioner =
                TestProvisioner(
                    TestProvisionersRegistry(resourceProvisioners = listOf(endpointProvisioner)),
                )

            val resource = EndpointResource("server", listOf(Endpoint("server", "10.0.0.1", 22, EndpointProtocol.ssh)))

            provisioner.waitForEndpointResult = { Error("wait failed for ${it.address}") }

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(
                provisioner
                    .apply(diffs, TestApplyContext())
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "wait failed for 10.0.0.1"
            }
        }
    }
}
