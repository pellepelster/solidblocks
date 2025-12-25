package solidblocks.test.gradle.ssh

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class SSHContext {

    @Test
    fun terraformExtension(context: SolidblocksTestContext) {
        val ssh = context.ssh("<ssh_host>", "<private_key>")

        val result = ssh.command("...")
    }
}
