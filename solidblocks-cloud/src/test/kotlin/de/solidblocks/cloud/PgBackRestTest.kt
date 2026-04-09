package de.solidblocks.cloud

import de.solidblocks.cloud.pgbackrest.model.parsePgBackRestInfoOutput
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class PgBackRestTest {
    @Test
    fun testParseInfoOutput() {
        val output = """
            [
              {
                "archive": [
                  {
                    "database": {
                      "id": 1,
                      "repo-key": 2
                    },
                    "id": "17-1",
                    "max": "000000010000000000000004",
                    "min": "000000010000000000000001"
                  }
                ],
                "backup": [
                  {
                    "archive": {
                      "start": "000000010000000000000002",
                      "stop": "000000010000000000000002"
                    },
                    "backrest": {
                      "format": 5,
                      "version": "2.58.0"
                    },
                    "database": {
                      "id": 1,
                      "repo-key": 2
                    },
                    "error": false,
                    "info": {
                      "delta": 24000143,
                      "repository": {
                        "delta": 3096734,
                        "size": 3096734
                      },
                      "size": 24000143
                    },
                    "label": "20260409-124922F",
                    "lsn": {
                      "start": "0/2000028",
                      "stop": "0/2000158"
                    },
                    "prior": null,
                    "reference": null,
                    "timestamp": {
                      "start": 1775738962,
                      "stop": 1775738986
                    },
                    "type": "full"
                  },
                  {
                    "archive": {
                      "start": "000000010000000000000004",
                      "stop": "000000010000000000000004"
                    },
                    "backrest": {
                      "format": 5,
                      "version": "2.58.0"
                    },
                    "database": {
                      "id": 1,
                      "repo-key": 2
                    },
                    "error": false,
                    "info": {
                      "delta": 31881374,
                      "repository": {
                        "delta": 4139565,
                        "size": 4139565
                      },
                      "size": 31881374
                    },
                    "label": "20260409-131304F",
                    "lsn": {
                      "start": "0/4000028",
                      "stop": "0/4000158"
                    },
                    "prior": null,
                    "reference": null,
                    "timestamp": {
                      "start": 1775740384,
                      "stop": 1775740401
                    },
                    "type": "full"
                  }
                ],
                "cipher": "none",
                "db": [
                  {
                    "id": 1,
                    "repo-key": 2,
                    "system-id": 7626740762852290622,
                    "version": "17"
                  }
                ],
                "name": "database1",
                "repo": [
                  {
                    "cipher": "none",
                    "key": 2,
                    "status": {
                      "code": 0,
                      "message": "ok"
                    }
                  }
                ],
                "status": {
                  "code": 0,
                  "lock": {
                    "backup": {
                      "held": false
                    },
                    "restore": {
                      "held": false
                    }
                  },
                  "message": "ok"
                }
              }
            ]
        """.trimIndent()

        assertSoftly(output.parsePgBackRestInfoOutput()) {
            it shouldHaveSize 1
            it[0].archive shouldHaveSize 1
            it[0].archive[0].id shouldBe "17-1"
            it[0].backup shouldHaveSize 2
            it[0].backup[0].timestamp.start shouldBe Instant.parse("2026-04-09T12:49:22Z")
            it[0].backup[0].timestamp.stop shouldBe Instant.parse("2026-04-09T12:49:46Z")
            it[0].backup[0].error shouldBe false
            it[0].backup[0].info.size shouldBe 24000143L
        }
    }
}
