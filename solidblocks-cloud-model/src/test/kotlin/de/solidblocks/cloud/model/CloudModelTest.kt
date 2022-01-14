package de.solidblocks.cloud.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CloudModelTest {

    @Test
    fun testGenerateSshKey() {
        val result = generateSshKey("test")
        assertThat(result.first).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(result.first).endsWith("-----END OPENSSH PRIVATE KEY-----\n")
        assertThat(result.second).startsWith("ssh-ed25519")
    }
}
