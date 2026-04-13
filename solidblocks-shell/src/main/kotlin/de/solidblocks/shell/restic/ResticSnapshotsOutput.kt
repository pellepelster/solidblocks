package de.solidblocks.shell.restic

import de.solidblocks.shell.json
import de.solidblocks.utils.Iso8601InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

typealias ResticSnapshots = List<ResticSnapshot>

@Serializable
data class ResticSnapshot(
    @Serializable(with = Iso8601InstantSerializer::class) val time: Instant,
    val tree: String,
    val paths: List<String>,
    val hostname: String,
    val username: String,
    @SerialName("program_version") val programVersion: String,
    val summary: ResticSummary,
    val id: String,
    @SerialName("short_id") val shortId: String,
)

@Serializable
data class ResticSummary(
    @Serializable(with = Iso8601InstantSerializer::class) @SerialName("backup_start") val backupStart: Instant,
    @Serializable(with = Iso8601InstantSerializer::class) @SerialName("backup_end") val backupEnd: Instant,
    @SerialName("files_new") val filesNew: Int,
    @SerialName("files_changed") val filesChanged: Int,
    @SerialName("files_unmodified") val filesUnmodified: Int,
    @SerialName("dirs_new") val dirsNew: Int,
    @SerialName("dirs_changed") val dirsChanged: Int,
    @SerialName("dirs_unmodified") val dirsUnmodified: Int,
    @SerialName("data_blobs") val dataBlobs: Int,
    @SerialName("tree_blobs") val treeBlobs: Int,
    @SerialName("data_added") val dataAdded: Long,
    @SerialName("data_added_packed") val dataAddedPacked: Long,
    @SerialName("total_files_processed") val totalFilesProcessed: Int,
    @SerialName("total_bytes_processed") val totalBytesProcessed: Long,
)

fun String.parseResticSnapshotsOutput(): ResticSnapshots = json.decodeFromString(this)
