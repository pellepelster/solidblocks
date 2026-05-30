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
data class ProtonPassListItem(
    @SerialName("id") val id: String,
    @SerialName("share_id") val shareId: String,
    @SerialName("title") val title: String,
)

@Serializable
data class ProtonPassListItemWrapper(@SerialName("items") val items: List<ProtonPassListItem>)

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

fun protonPassTest() = runCommand(listOf("pass-cli", "test"))

fun protonPassVaultList() = runCommand(listOf("pass-cli", "vault", "list", "--output", "json"))

fun protonPassVaultCreate(name: String) = runCommand(listOf("pass-cli", "vault", "create", "--name", name))

fun protonPassItemView(vaultName: String, title: String) = runCommand(
    listOf("pass-cli", "item", "view", "--vault-name", vaultName, "--item-title", title, "--output", "json"),
)

fun protonPassItemList(vaultName: String) = runCommand(
    listOf("pass-cli", "item", "list", "--output", "json", vaultName),
)

fun protonPassItemCreateNote(vaultName: String, title: String, secret: String) = runCommand(
    listOf("pass-cli", "item", "create", "note", "--vault-name", vaultName, "--from-template", "-"),
    stdin = Json.encodeToString(ProtonPassNoteTemplate(title, secret)),
)

fun protonPassItemDelete(shareId: String, itemId: String) = runCommand(
    listOf("pass-cli", "item", "delete", "--share-id", shareId, "--item-id", itemId),
)

fun parseProtonPassItem(stdout: String): ProtonPassItem? = try {
    protonPassJson.decodeFromString<ProtonPassItemViewWrapper>(stdout).item
} catch (e: Exception) {
    null
}

fun parseProtonPassList(stdout: String): ProtonPassListItemWrapper? = try {
    protonPassJson.decodeFromString<ProtonPassListItemWrapper>(stdout)
} catch (e: Exception) {
    null
}

fun parseProtonPassVaults(stdout: String): List<ProtonPassVault>? = try {
    protonPassJson.decodeFromString<ProtonPassVaultListWrapper>(stdout).vaults
} catch (e: Exception) {
    null
}
