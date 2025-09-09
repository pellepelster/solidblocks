package de.solidblocks.cli

import de.solidblocks.cli.hetzner.HetznerLabels
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

class HetznerLabelsTest {

    @Test
    fun testAddHashedLabel() {

        val labels = HetznerLabels()

        labels.addHashedLabel("hash-test", "hallo welt")
        labels.labels()["hash-test"] shouldBe "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"

        assertSoftly(labels.hashLabelMatches("hash-test", "hallo welt")) {
            it.matches shouldBe true
        }

        assertSoftly(labels.hashLabelMatches("hash-test", "hallo welt1")) {
            it.matches shouldBe false
        }
    }

    @Test
    fun testLabelExportImport() {

        val labels = HetznerLabels()

        labels.addHashedLabel("hash-test", "hallo welt")
        labels.addHashedLabel("long-test", "A".repeat(124))
        labels.labels()["hash-test"] shouldBe "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"

        val map = labels.rawLabels()

        val importedLabels = HetznerLabels(map)
        importedLabels.labels()["hash-test"] shouldBe "028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455"
        labels.addHashedLabel("long-test", "A".repeat(124))
    }

    @Test
    fun testMaxLabelValue() {
        val labels = HetznerLabels()
        labels.addLabel("label1", "A".repeat(124))

        labels.labels() shouldHaveSize 1
        labels.labels()["label1"] shouldBe "A".repeat(124)

        labels.rawLabels()["label1_0"] shouldBe "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        labels.rawLabels()["label1_1"] shouldBe "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    }

    @Test
    fun testTooLongLabelValue() {
        val labels = HetznerLabels()

        assertThrows(
            RuntimeException::class.java
        ) {
            labels.addLabel("label1", "A".repeat(125))
        }
    }

    @Test
    fun testUnderscoreInKey() {
        val labels = HetznerLabels()

        assertThrows(
            RuntimeException::class.java
        ) {
            labels.addLabel("label_1", "value1")
        }
    }
}
