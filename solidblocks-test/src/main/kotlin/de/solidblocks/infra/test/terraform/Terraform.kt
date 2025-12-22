package de.solidblocks.infra.test.terraform

import de.solidblocks.infra.test.LogType
import de.solidblocks.infra.test.detectGolangPlatform
import de.solidblocks.infra.test.log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

const val TERRAFORM_DEFAULT_VERSION = "1.14.2"

fun testTerraform(dir: Path, version: String? = null) = TerraformTestContext(dir, version)

class TerraformTestContext(
    val dir: Path,
    version: String? = null,
    val environment: Map<String, String> = emptyMap(),
) {

  val terraform = Terraform(dir, version, environment)

  init {
    if (!dir.exists()) {
      throw RuntimeException("Terraform dir '$dir' does not exist")
    }
    terraform.ensureTerraformBinary()
  }

  fun apply() = terraform.apply()

  fun destroy() = terraform.destroy()

  fun version() = terraform.version()

  fun init() = terraform.init()

  fun output() = terraform.output()

  fun deleteLocalState() = terraform.deleteLocalState()

  fun addVariable(name: String, value: Any) = terraform.addVariable(name, value)
}

class Terraform(
    val dir: Path,
    versionOverride: String? = null,
    val environment: Map<String, String> = emptyMap(),
) {

  val version = versionOverride ?: TERRAFORM_DEFAULT_VERSION
  val platform = detectGolangPlatform()
  val cacheDir = Paths.get("").resolve(".cache").toAbsolutePath()
  val binDir = Paths.get("").resolve(".bin").resolve(this.version).toAbsolutePath()
  val osName = System.getProperty("os.name").lowercase()

  val terraformBinaryFileName =
      when {
        osName.contains("win") -> "terraform.exe"
        else -> "terraform"
      }
  val terraformBinary = binDir.resolve(terraformBinaryFileName)

  init {
    cacheDir.toFile().mkdirs()
    binDir.toFile().mkdirs()
  }

  val variables = mutableMapOf<String, String>()

  fun addVariable(name: String, value: Any) {
    variables[name] = value.toString()
  }

  fun ensureTerraformBinary() {
    downloadTerraform()
  }

  fun downloadTerraform(): File {
    val baseUrl = "https://releases.hashicorp.com/terraform/$version"
    val filename = "terraform_${version}_${platform.os}_${platform.arch}.zip"

    val zipFile = cacheDir.resolve(filename).toFile()
    val checksumFile = cacheDir.resolve("terraform_${version}_SHA256SUMS").toFile()

    if (checksumFile.exists()) {
      log("Terraform checksums already downloaded at '$checksumFile'")
    } else {
      val checksumsUrl = "$baseUrl/terraform_${version}_SHA256SUMS"
      log("downloading Terraform checksums from '$checksumsUrl' to '$checksumFile")
      downloadFile(checksumsUrl, checksumFile)
    }

    if (!checksumMatches(checksumFile, zipFile)) {
      log("downloading Terraform '$version' for '${platform.os}/${platform.arch}'")
      downloadFile("$baseUrl/$filename", zipFile)
    } else {
      log("Terraform already downloaded at '$zipFile'")
    }

    if (!checksumMatches(checksumFile, zipFile)) {
      throw RuntimeException(
          "checksum mismatch, '$zipFile' did not match Terraform checksums from '$checksumFile'",
      )
    }

    val terraformBinary = extractTerraform(zipFile, terraformBinary.toFile())
    log("Terraform installed at '${terraformBinary.absolutePath}'")

    return terraformBinary
  }

  private fun downloadFile(urlString: String, outputFile: File) {
    URL(urlString).openStream().use { input ->
      outputFile.outputStream().use { output -> input.copyTo(output) }
    }
  }

  private fun calculateSHA256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
      val buffer = ByteArray(8192)
      var bytesRead = input.read(buffer)
      while (bytesRead != -1) {
        digest.update(buffer, 0, bytesRead)
        bytesRead = input.read(buffer)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private fun extractChecksum(checksumFile: File, filename: String) =
      checksumFile
          .readLines()
          .first { it.contains(filename) }
          .split("\\s+".toRegex())
          .first()
          .lowercase()

  private fun checksumMatches(checksumFile: File, file: File) =
      file.exists() && extractChecksum(checksumFile, file.name) == calculateSHA256(file)

  private fun extractTerraform(zipFile: File, targetFile: File): File {
    ZipInputStream(zipFile.inputStream()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (entry.name == terraformBinaryFileName) {
          log("Extracting Terraform binary to '$targetFile'")
          targetFile.outputStream().use { output -> zip.copyTo(output) }
          targetFile.setExecutable(true)
          return targetFile
        }
        entry = zip.nextEntry
      }
    }

    throw RuntimeException("Terraform binary not found in zip file '$zipFile'")
  }

  fun apply() {
    run(listOf("apply", "-auto-approve", "-input=false"))
  }

  fun destroy() {
    run(listOf("destroy", "-auto-approve"))
  }

  fun init() {
    run(listOf("init", "-upgrade"))
  }

  fun version() = run(listOf("-version")).stdout

  @OptIn(ExperimentalSerializationApi::class)
  fun output(): TerraformOutput {
    val result = run(listOf("output", "-json"), false)

    return TerraformOutput(
        Json.decodeFromString<Map<String, OutputVariable>>(result.stdout),
    )
  }

  private data class Result(val stdout: String, val stderr: String)

  private fun run(
      command: List<String>,
      streamLog: Boolean = true,
  ): Result = runBlocking {
    log(
        "running '$terraformBinary ${command.joinToString(" ")}' in '$dir'",
    )

    val processBuilder =
        ProcessBuilder()
            .command(listOf(terraformBinary.toAbsolutePath().toString()) + command)
            .directory(dir.toAbsolutePath().toFile())

    val stdOut = StringBuilder()
    val stdErr = StringBuilder()

    processBuilder.environment().putAll(environment)
    processBuilder.environment().putAll(variables.mapKeys { "TF_VAR_${it.key}" })
    val process = processBuilder.start()

    val stdOutJob =
        launch(Dispatchers.IO) {
          BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lineSequence().forEach {
              if (streamLog) {
                log(it, LogType.STDOUT)
              }
              stdOut.append(it)
            }
          }
        }

    val stdErrJob =
        launch(Dispatchers.IO) {
          BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            reader.lineSequence().forEach {
              if (streamLog) {
                log(it, LogType.STDERR)
              }
              stdErr.append(it)
            }
          }
        }

    if (!process.waitFor(5, TimeUnit.MINUTES)) {
      process.destroyForcibly()
    }

    val exitCode = process.exitValue()

    stdOutJob.join()
    stdErrJob.join()

    if (exitCode != 0) {
      throw Exception(
          "Terraform command '${command.joinToString(" ")}' failed, stdout was '$stdOut', stderr '$stdErr'",
      )
    }

    Result(stdOut.toString(), stdErr.toString())
  }

  fun deleteLocalState() {
    val stateFile = dir.resolve("terraform.tfstate")
    if (stateFile.exists()) {
      log("removing Terraform state '$stateFile'")
      if (!stateFile.toFile().delete()) {
        throw RuntimeException("failed to remove Terraform state '$stateFile'")
      }
    }
  }
}
