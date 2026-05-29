package de.solidblocks.cloud.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val protonPassJson = Json { ignoreUnknownKeys = true }

@Serializable
data class ProtonPassItemViewWrapper(@SerialName("item") val item: ProtonPassItem)

@Serializable
data class ProtonPassItem(
    @SerialName("id") val id: String,
    @SerialName("share_id") val shareId: String,
    @SerialName("content") val content: ProtonPassItemContent,
)

@Serializable
data class ProtonPassItemContent(
    @SerialName("title") val title: String,
    @SerialName("note") val note: String? = null,
)

@Serializable
data class ProtonPassVaultListWrapper(@SerialName("vaults") val vaults: List<ProtonPassVault>)

@Serializable
data class ProtonPassVault(
    @SerialName("name") val name: String,
    @SerialName("vault_id") val vaultId: String,
    @SerialName("share_id") val shareId: String,
)

@Serializable
private data class ProtonPassNoteTemplate(
    @SerialName("title") val title: String,
    @SerialName("note") val note: String,
)

/**
 * Tests if an authenticated connection to Proton Pass can be established.
 */
fun protonPassTest() = runCommand(listOf("pass-cli", "test"))

/**
 * Lists all available vaults as JSON.
 */
fun protonPassVaultList() = runCommand(listOf("pass-cli", "vault", "list", "--output", "json"))

/**
 * Reads a note item from a vault as JSON. Exits non-zero if the item does not exist.
 */
fun protonPassItemView(vaultName: String, title: String) = runCommand(
    listOf("pass-cli", "item", "view", "--vault-name", vaultName, "--item-title", title, "--output", "json"),
)

/**
 * Creates a note item in a vault. The secret value is passed via stdin (JSON template) to avoid
 * leaking it through the process argument list.
 */
fun protonPassItemCreateNote(vaultName: String, title: String, secret: String) = runCommand(
    listOf("pass-cli", "item", "create", "note", "--vault-name", vaultName, "--from-template", "-"),
    stdin = Json.encodeToString(ProtonPassNoteTemplate(title, secret)),
)

/**
 * Deletes an item by its share and item id.
 */
fun protonPassItemDelete(shareId: String, itemId: String) = runCommand(
    listOf("pass-cli", "item", "delete", "--share-id", shareId, "--item-id", itemId),
)

fun parseProtonPassItem(stdout: String): ProtonPassItem? = try {
    protonPassJson.decodeFromString<ProtonPassItemViewWrapper>(stdout).item
} catch (e: Exception) {
    null
}

fun parseProtonPassVaults(stdout: String): List<ProtonPassVault>? = try {
    protonPassJson.decodeFromString<ProtonPassVaultListWrapper>(stdout).vaults
} catch (e: Exception) {
    null
}
