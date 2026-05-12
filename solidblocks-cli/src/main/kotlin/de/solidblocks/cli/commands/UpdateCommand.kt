package de.solidblocks.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import de.solidblocks.cloud.utils.solidblocksVersion
import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class UpdateCommand : CliktCommand(name = "update") {

    @Serializable
    private data class GitHubRelease(@SerialName("tag_name") val tagName: String)

    override fun help(context: Context) = "Update blcks to the latest version"

    override fun run() {
        val currentVersion = solidblocksVersion()

        val latestTag = runBlocking { fetchLatestTag() } ?: run {
            logError("Failed to fetch latest release information")
            return
        }
        val latestVersion = latestTag.removePrefix("v")

        if (!isNewer(latestVersion, currentVersion)) {
            logInfo("Already on latest version $currentVersion")
            return
        }

        logInfo("newer version available: $latestVersion (current: $currentVersion)")

        val platform = detectPlatform() ?: run {
            logError("unsupported platform: ${System.getProperty("os.name")}/${System.getProperty("os.arch")}")
            return
        }

        val currentBinary = currentBinaryPath() ?: run {
            logError("Unable to determine current binary path")
            return
        }

        val downloadUrl = "https://github.com/pellepelster/solidblocks/releases/download/$latestTag/blcks-cli-$platform-$latestTag.zip"

        runBlocking { downloadAndReplace(downloadUrl, currentBinary, platform, latestVersion) }
    }

    private suspend fun fetchLatestTag(): String? = try {
        HttpClient(Java) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }.use { client ->
            val response = client.get("https://api.github.com/repos/pellepelster/solidblocks/releases/latest") {
                headers {
                    append(HttpHeaders.Accept, "application/vnd.github+json")
                    append("X-GitHub-Api-Version", "2022-11-28")
                }
            }
            if (!response.status.isSuccess()) null else response.body<GitHubRelease>().tagName
        }
    } catch (_: Exception) {
        null
    }

    private fun detectPlatform(): String? {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val osStr = when {
            os.contains("linux") -> "linux"
            os.contains("mac") || os.contains("darwin") -> "darwin"
            os.contains("windows") -> "windows"
            else -> return null
        }

        val archStr = when (arch) {
            "amd64", "x86_64" -> "amd64"
            "aarch64", "arm64" -> "arm64"
            else -> return null
        }

        return "$osStr-$archStr"
    }

    private fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
        val l = parts(latest)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val diff = l.getOrElse(i) { 0 } - c.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    private fun currentBinaryPath(): File? = ProcessHandle.current().info().command()
        .map { File(it) }
        .orElse(null)
        ?.takeIf { it.exists() }

    private suspend fun downloadAndReplace(downloadUrl: String, currentBinary: File, platform: String, version: String) {
        val binaryName = if (platform.startsWith("windows")) "blcks.exe" else "blcks"
        val tempZip = Files.createTempFile("blcks-update-", ".zip").toFile()

        try {
            logInfo("Downloading '$downloadUrl'")
            HttpClient(Java).use { client ->
                val response = client.get(downloadUrl)
                if (!response.status.isSuccess()) {
                    logError("Download failed: HTTP ${response.status}")
                    return
                }
                tempZip.writeBytes(response.body())
            }

            val tempBinary = Files.createTempFile("blcks-new-", "").toFile()
            try {
                var extracted = false
                ZipInputStream(tempZip.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == binaryName) {
                            tempBinary.outputStream().use { zip.copyTo(it) }
                            extracted = true
                            break
                        }
                        entry = zip.nextEntry
                    }
                }

                if (!extracted || tempBinary.length() == 0L) {
                    logError("Failed to extract binary from zip")
                    return
                }

                tempBinary.setExecutable(true)
                Files.move(tempBinary.toPath(), currentBinary.toPath(), StandardCopyOption.REPLACE_EXISTING)
                logInfo("updated to $version")
            } finally {
                tempBinary.delete()
            }
        } finally {
            tempZip.delete()
        }
    }
}
