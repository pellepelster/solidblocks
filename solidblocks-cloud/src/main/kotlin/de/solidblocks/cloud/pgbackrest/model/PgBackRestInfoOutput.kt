package de.solidblocks.cloud.pgbackrest.model

import de.solidblocks.cloud.utils.EpochSecondInstantSerializer
import de.solidblocks.cloud.utils.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

typealias PgBackRestInfo = List<Stanza>

@Serializable
data class Stanza(
    val archive: List<ArchiveEntry>,
    val backup: List<Backup>,
    val cipher: String,
    val db: List<DbEntry>,
    val name: String,
    val repo: List<Repo>,
    val status: StanzaStatus,
)

@Serializable
data class ArchiveEntry(
    val database: DatabaseRef,
    val id: String,
    val max: String,
    val min: String,
)

@Serializable
data class Backup(
    val archive: BackupArchiveRange,
    val backrest: Backrest,
    val database: DatabaseRef,
    val error: Boolean,
    val info: BackupInfo,
    val label: String,
    val lsn: Lsn,
    val prior: String?,
    val reference: String?,
    val timestamp: Timestamp,
    val type: String,
)

@Serializable
data class BackupArchiveRange(
    val start: String,
    val stop: String,
)

@Serializable
data class Backrest(
    val format: Int,
    val version: String,
)

@Serializable
data class BackupInfo(
    val delta: Long,
    val repository: RepositoryInfo,
    val size: Long,
)

@Serializable
data class RepositoryInfo(
    val delta: Long,
    val size: Long,
)

@Serializable
data class Lsn(
    val start: String,
    val stop: String,
)

@Serializable
data class Timestamp(
    @Serializable(with = EpochSecondInstantSerializer::class) val start: Instant,
    @Serializable(with = EpochSecondInstantSerializer::class) val stop: Instant,
)

@Serializable
data class DbEntry(
    val id: Int,
    @SerialName("repo-key") val repoKey: Int,
    @SerialName("system-id") val systemId: Long,
    val version: String,
)

@Serializable
data class Repo(
    val cipher: String,
    val key: Int,
    val status: RepoStatus,
)

@Serializable
data class RepoStatus(
    val code: Int,
    val message: String,
)

@Serializable
data class StanzaStatus(
    val code: Int,
    val lock: Lock,
    val message: String,
)

@Serializable
data class Lock(
    val backup: LockHeld,
    val restore: LockHeld,
)

@Serializable
data class LockHeld(
    val held: Boolean,
)

@Serializable
data class DatabaseRef(
    val id: Int,
    @SerialName("repo-key") val repoKey: Int,
)

fun String.parsePgBackRestInfoOutput(): PgBackRestInfo = json.decodeFromString(this)
