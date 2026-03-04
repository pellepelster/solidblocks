package solidblocks.test.gradle.ssh

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.io.path.Path

@ExtendWith(SolidblocksTest::class)
public class SSHAssertions {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val ssh = context.ssh("<ssh_host>", "<private_key>")

        // run a command remotely
        ssh.command("whoami").stdout shouldBe "root"

        // ensure that a remote file exists
        ssh.fileExists("<file_path>") shouldBe true

        // check permissions of a remote file
        ssh.filePermissions("<file_path>") shouldBe "-rwx---r-x"

        // retrieve file and check content
        ssh.download("<file_path>") shouldBe "foo-bar".toByteArray()

        // upload a file to a remote server
        ssh.upload(Path("<local_file>"), "<remote_file_path>")

    }
}
