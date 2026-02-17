package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.api.ResourceDiffStatus.*
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.provisioner.mock.Resource1
import de.solidblocks.cloud.provisioner.mock.Resource1Provisioner
import de.solidblocks.cloud.provisioner.mock.Resource2
import de.solidblocks.cloud.provisioner.mock.Resource2Provisioner
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.Waiter
import io.kotest.assertions.assertSoftly
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.junit.jupiter.api.Test

class ProvisionerTest {

  @Test
  fun testDiffsCanFailIfParentResourceMissing() {
    runBlocking {
      val provisioner =
          Provisioner(
              ProvisionersRegistry(
                  emptyList(),
                  listOf(Resource1Provisioner(), Resource2Provisioner()),
              ),
              Waiter(1, 1.seconds.toJavaDuration()),
          )

      val resource1 = Resource1("test1")
      val resource2Fail = Resource2("throw_exception_on_diff", setOf(resource1))
      val resource2 = Resource2("test2", setOf(resource1))

      val diffs =
          provisioner
              .diff(
                  listOf(ResourceGroup("common", listOf(resource1, resource2Fail))),
                  TEST_PROVISIONER_CONTEXT,
                  TEST_LOG_CONTEXT,
              )
              .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
              .data

      assertSoftly(diffs) {
        it.entries shouldHaveSize 1
        it.values.first()[0].resource.name shouldBe "test1"
        it.values.first()[0].status shouldBe missing
        it.values.first()[1].resource.name shouldBe "throw_exception_on_diff"
        it.values.first()[1].status shouldBe parent_missing
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
                  TEST_PROVISIONER_CONTEXT,
                  TEST_LOG_CONTEXT,
              )
              .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>(),
      ) {
        it.data.entries shouldHaveSize 1
        it.data.values.first() shouldHaveSize 2
        it.data.values.first()[0].status shouldBe ResourceDiffStatus.up_to_date
      }
    }
  }

  @Test
  fun testResourcesAreRecreatedIfNeeded() {
    runBlocking {
      val resource2Provisioner = Resource2Provisioner()

      val provisioner =
          Provisioner(
              ProvisionersRegistry(emptyList(), listOf(resource2Provisioner)),
              Waiter(1, 1.seconds.toJavaDuration()),
          )

      val resource2ForceRecreateChange = Resource2("force_recreate_change", setOf())

      val diffs =
          provisioner
              .diff(
                  listOf(
                      ResourceGroup("common", listOf(resource2ForceRecreateChange)),
                  ),
                  TEST_PROVISIONER_CONTEXT,
                  TEST_LOG_CONTEXT,
              )
              .shouldBeTypeOf<Success<Map<ResourceGroup, List<ResourceDiff>>>>()
              .data

      assertSoftly(diffs) {
        it.entries shouldHaveSize 1
        it.values.first()[0].resource.name shouldBe "force_recreate_change"
        it.values.first()[0].status shouldBe has_changes
        it.values.first()[0].needsRecreate() shouldBe true
      }

      provisioner
          .apply(diffs, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<Unit>>()
      resource2Provisioner.isDestroyed("force_recreate_change") shouldBe true
      resource2Provisioner.isApplied("force_recreate_change") shouldBe true
    }
  }
}
