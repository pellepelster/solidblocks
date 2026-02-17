package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.cloudLabel
import de.solidblocks.cloud.Constants.managedByLabel
import de.solidblocks.cloud.Constants.solidblocksVersion
import de.solidblocks.cloud.Constants.versionLabel
import de.solidblocks.cloud.utils.HetznerLabels
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HetznerLabelsTest {

  @Test
  fun hasDefaultLabels() {
    val labels = HetznerLabels.forCloud("test")
    labels.labels() shouldHaveSize 3
    labels.labels()[cloudLabel] shouldBe "test"
    labels.labels()[managedByLabel] shouldBe "blcks"
    labels.labels()[versionLabel] shouldBe solidblocksVersion()
  }

  @Test
  fun testAddHashedLabel() {
    val labels = HetznerLabels.forCloud("test")

    labels.addHashedLabel("hash-test", "hallo welt")
    labels.labels()["hash-test"] shouldBe
        "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"

    assertSoftly(labels.hashLabelMatches("hash-test", "hallo welt")) { it.matches shouldBe true }

    assertSoftly(labels.hashLabelMatches("hash-test", "hallo welt1")) { it.matches shouldBe false }
  }

  @Test
  fun testLabelExportImport() {
    val labels = HetznerLabels.forCloud("test")

    labels.addHashedLabel("hash-test", "hallo welt")
    labels.addHashedLabel("long-test", "A".repeat(124))
    labels.labels()["hash-test"] shouldBe
        "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"

    val map = labels.rawLabels()

    val importedLabels = HetznerLabels(map)
    importedLabels.labels()["hash-test"] shouldBe
        "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"
    labels.addHashedLabel("long-test", "A".repeat(124))
  }

  @Test
  fun testMaxLabelValue() {
    val labels = HetznerLabels.forCloud("test")
    labels.addLabel("label1", "A".repeat(124))

    labels.labels() shouldHaveSize 4
    labels.labels()["label1"] shouldBe "A".repeat(124)

    labels.rawLabels()["label1_0"] shouldBe
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    labels.rawLabels()["label1_1"] shouldBe
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
  }

  @Test
  fun testTooLongLabelValue() {
    val labels = HetznerLabels.forCloud("test")

    assertThrows(
        RuntimeException::class.java,
    ) {
      labels.addLabel("label1", "A".repeat(125))
    }
  }

  @Test
  fun testUnderscoreInKey() {
    val labels = HetznerLabels.forCloud("test")

    assertThrows(
        RuntimeException::class.java,
    ) {
      labels.addLabel("label_1", "value1")
    }
  }
}
