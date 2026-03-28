package de.solidblocks.shell

import java.io.BufferedReader

object ResticLibrary {
  fun source() =
      ResticLibrary::class
          .java
          .classLoader
          .getResourceAsStream("restic.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )

  class Install : LibraryCommand {
    override fun commands() = listOf("restic_install")
  }

  class EnsureLocalRepo(val repository: String) : LibraryCommand {
    override fun commands() = listOf("restic_ensure_local_repo '$repository'")
  }

  class EnsureS3Repo(val repository: String) : LibraryCommand {
    override fun commands() = listOf("restic_ensure_s3_repo '$repository'")
  }

  const val RESTIC_CREDENTIALS_PATH = "/etc/restic/credentials"

  class WriteS3Credentials(val password: String, val accessKey: String, val secretKey: String) :
      LibraryCommand {
    override fun commands() =
        MkDir("/etc/restic/").commands() +
            WriteFile(
                    """
                        RESTIC_PASSWORD="$password"
                        AWS_ACCESS_KEY_ID="$accessKey"
                        AWS_SECRET_ACCESS_KEY="$secretKey"
                """
                        .trimIndent()
                        .toByteArray(Charsets.UTF_8),
                    RESTIC_CREDENTIALS_PATH,
                    FilePermissions.RW_R__R__,
                )
                .commands()
  }

  class WriteCredentials(val password: String) : LibraryCommand {
    override fun commands() =
        MkDir("/etc/restic/").commands() +
            WriteFile(
                    """
                        RESTIC_PASSWORD="$password"
                """
                        .trimIndent()
                        .toByteArray(Charsets.UTF_8),
                    RESTIC_CREDENTIALS_PATH,
                    FilePermissions.RW_R__R__,
                )
                .commands()
  }

  class Backup(val repository: String, val directory: String) : LibraryCommand {
    override fun commands() = listOf("restic_backup '$repository' '$directory'")
  }

  class Restore(val repository: String) : LibraryCommand {
    override fun commands() = listOf("restic_restore '$repository'")
  }
}
