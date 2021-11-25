package de.solidblocks.provisioner.vault

import de.solidblocks.provisioner.vault.ssh.COMPARE_VAULT_TTL
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultUtilsTest {

    @Test
    fun testCompareVaultTtl() {
        assertTrue(COMPARE_VAULT_TTL.invoke("1h", "3600"))
        assertTrue(COMPARE_VAULT_TTL.invoke("3600", "1h"))
        assertFalse(COMPARE_VAULT_TTL.invoke("", "1h"))
        assertFalse(COMPARE_VAULT_TTL.invoke("1h", ""))
    }
}
