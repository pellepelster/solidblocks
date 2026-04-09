package de.solidblocks.cloud

import de.solidblocks.cloud.pgbackrest.model.parsePgBackRestInfoOutput
import de.solidblocks.cloud.restic.model.parseResticSnapshotsOutput
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class ResticTest {
    @Test
    fun testParseInfoOutput() {
        val output = """
[
  {
    "time": "2026-04-09T14:03:47.485956603Z",
    "tree": "344492aa412832d101c1174a8d8eadf09358b80ec6db004767c95032cdc025fc",
    "paths": [
      "/storage/data/service1"
    ],
    "hostname": "cloud1-default-service1-0",
    "username": "root",
    "program_version": "restic 0.18.1",
    "summary": {
      "backup_start": "2026-04-09T14:03:47.485956603Z",
      "backup_end": "2026-04-09T14:03:48.159665567Z",
      "files_new": 1,
      "files_changed": 0,
      "files_unmodified": 0,
      "dirs_new": 5,
      "dirs_changed": 0,
      "dirs_unmodified": 0,
      "data_blobs": 1,
      "tree_blobs": 6,
      "data_added": 2245,
      "data_added_packed": 1849,
      "total_files_processed": 1,
      "total_bytes_processed": 107
    },
    "id": "b1f95ac33f83d2eacc3a50638c41bed8d31aa8be75a80db9944f356987b30ddb",
    "short_id": "b1f95ac3"
  }]
        """.trimIndent()

        assertSoftly(output.parseResticSnapshotsOutput()) {
            it shouldHaveSize 1
        }
    }
}
