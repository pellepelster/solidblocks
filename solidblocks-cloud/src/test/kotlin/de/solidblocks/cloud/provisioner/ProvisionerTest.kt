package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.provisioner.mock.ApplyBehaviour.error_on_apply
import de.solidblocks.cloud.provisioner.mock.ApplyBehaviour.throw_exception_on_apply
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.duplicate_on_diff
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.error_on_diff
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.force_recreate_change
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.throw_exception_on_diff
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.unknown_on_diff
import de.solidblocks.cloud.provisioner.mock.DiffBehaviour.up_to_date_or_missing
import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Provisioner
import de.solidblocks.cloud.provisioner.mock.Resource2
import de.solidblocks.cloud.provisioner.mock.Resource2Provisioner
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.WaitConfig
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ProvisionerTest {

    @Test
    fun `resource is tainted if parent is missing`() {
        val resource1Provisioner = Resource1Provisioner()
        runBlocking {
            val provisioner =
                Provisioner(
                    ProvisionersRegistry(
                        emptyList(),
                        listOf(resource1Provisioner, Resource2Provisioner()),
                    ),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource1 = Resource1("test1")
            val resource2 = Resource2("test2", up_to_date_or_missing, setOf(resource1))

            assertSoftly(resource1Provisioner.diff(resource1, TEST_PROVISIONER_CONTEXT).shouldBeInstanceOf<Success<ResourceDiff>>()) {
                it.data.status shouldBe missing
            }

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource1, resource2))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.entries shouldHaveSize 1
                it.values.first()[0].resource.name shouldBe "test1"
                it.values.first()[0].status shouldBe missing
                it.values.first()[1].resource.name shouldBe "test2"
                it.values.first()[1].status shouldBe tainted
            }

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<Unit>>()

            assertSoftly(
                provisioner
                    .diff(
                        listOf(
                            ResourceGroup("common", listOf(resource1, resource2)),
                        ),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("error_on_diff", error_on_diff)

            provisioner
                .diff(
                    listOf(ResourceGroup("common", listOf(resource))),
                    { false },
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                )
                .shouldBeTypeOf<Error<Map<ResourceGroup, List<ResourceDiff>>>>()
        }
    }

    @Test
    fun `unexpected exception during diff aborts plan`() {
        runBlocking {
            val provisioner =
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("throw_exception_on_diff", throw_exception_on_diff)

            provisioner
                .diff(
                    listOf(ResourceGroup("common", listOf(resource))),
                    { false },
                    TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)
            val resource2ForceRecreateChange = Resource2(resourceName, force_recreate_change)

            provisioner
                .apply(
                    listOf(resource2),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(
                            ResourceGroup("common", listOf(resource2ForceRecreateChange)),
                        ),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
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
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe true
            resource2Provisioner.isApplied(resourceName) shouldBe true
        }
    }

    @Test
    fun `unknown diff status does not abort the plan`() {
        runBlocking {
            val provisioner =
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("unknown_on_diff", unknown_on_diff)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("duplicate_on_diff", duplicate_on_diff)

            assertSoftly(
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)

            provisioner
                .apply(
                    listOf(resource2),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_PROVISIONER_CONTEXT,
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
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing, taintRequiresRecreate = true)

            provisioner
                .apply(
                    listOf(resource2),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].status shouldBe tainted
                it.values.first()[0].needsRecreate() shouldBe true
            }

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing, taintable = false)

            provisioner
                .apply(
                    listOf(resource2),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { true },
                        TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val parent = Resource2(parentName, up_to_date_or_missing)
            val child = Resource2(childName, up_to_date_or_missing, setOf(parent))

            provisioner
                .apply(
                    listOf(parent, child),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(parent, child))),
                        { it.name == parentName },
                        TEST_PROVISIONER_CONTEXT,
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("duplicate", up_to_date_or_missing)
            val diffs = mapOf(
                ResourceGroup("common", listOf(resource)) to
                    listOf(ResourceDiff(resource, duplicate, duplicateErrorMessage = "duplicate error")),
            )

            assertSoftly(
                provisioner
                    .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                    .shouldBeTypeOf<Error<Unit>>(),
            ) {
                it.error shouldBe "duplicate error"
            }
        }
    }

    @Test
    fun `failed destroy aborts apply`() {
        runBlocking {
            val resource2Provisioner = Resource2Provisioner()
            val resourceName = UUID.randomUUID().toString()

            val provisioner =
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            provisioner
                .apply(
                    listOf(Resource2(resourceName, up_to_date_or_missing)),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(Resource2(resourceName, force_recreate_change)))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            resource2Provisioner.destroyResult = false

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(Resource2(resourceName, force_recreate_change)))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.isDestroyed(resourceName) shouldBe false
            resource2Provisioner.isApplied(resourceName) shouldBe true
        }
    }

    @Test
    fun `error during apply aborts rollout`() {
        runBlocking {
            val provisioner =
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("error_on_apply", up_to_date_or_missing, applyBehaviour = error_on_apply)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(
                provisioner
                    .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("throw_exception_on_apply", up_to_date_or_missing, applyBehaviour = throw_exception_on_apply)

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Error<Unit>>()
        }
    }

    @Test
    fun `exception during apply of resource list is reported as error`() {
        runBlocking {
            val provisioner =
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(Resource2Provisioner())),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource = Resource2("throw_exception_on_apply", up_to_date_or_missing, applyBehaviour = throw_exception_on_apply)

            assertSoftly(
                provisioner
                    .apply(listOf(resource), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
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
                Provisioner(
                    ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
                    emptyList(),
                    WaitConfig(1, 1.seconds),
                )

            val resource2 = Resource2(resourceName, up_to_date_or_missing)

            provisioner
                .apply(
                    listOf(resource2),
                    TEST_PROVISIONER_CONTEXT,
                    TEST_LOG_CONTEXT,
                ).shouldBeInstanceOf<Success<Unit>>()

            val diffs =
                provisioner
                    .diff(
                        listOf(ResourceGroup("common", listOf(resource2))),
                        { false },
                        TEST_PROVISIONER_CONTEXT,
                        TEST_LOG_CONTEXT,
                    )
                    .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
                    .data

            assertSoftly(diffs) {
                it.values.first()[0].status shouldBe up_to_date
            }

            provisioner
                .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<Unit>>()

            resource2Provisioner.applyCount(resourceName) shouldBe 1
        }
    }
}
