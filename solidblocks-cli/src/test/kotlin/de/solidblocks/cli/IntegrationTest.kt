package de.solidblocks.cli

import org.junit.jupiter.api.Test

class IntegrationTest {

  @Test
  fun testS3() {
    val yaml =
        """
        ---
        providers:
          - type: pass
            path: /tmp/yolo

        services:
          - name: public
            type: s3
        """
            .trimIndent()
  }
}
