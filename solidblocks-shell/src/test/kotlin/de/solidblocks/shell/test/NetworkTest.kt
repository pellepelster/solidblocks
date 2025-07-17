package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.testDocker
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import java.util.*

public class NetworkTest {
    @Test
    fun testNetworkWaitForPortOpen() {
        val result =
            testDocker(DockerTestImage.DEBIAN_10)
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("network.sh"))
                .step(
                    "network_wait_for_port_open pelle.io 22",
                ).run()

        assertSoftly(result) { it shouldHaveExitCode 0 }
    }
}
