package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.shell.AptLibrary
import de.solidblocks.shell.CloudInit
import de.solidblocks.shell.DockerLibrary
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.StorageLibrary
import de.solidblocks.shell.toCloudInit
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class CloudInitTest {

    @Test
    fun testRender() {
        val cloudInit = CloudInit()

        cloudInit.addFile("/usr/lib/blcks/mock-test.sh", "line 1\nline 2", "0755")

        cloudInit.addRunCommand("foo")
        cloudInit.addRunCommand("bar")
        cloudInit.render() shouldBe """
            #cloud-config
            write_files:
                - path: /usr/lib/blcks/mock-test.sh
                  permissions: 0755
                  content: |
                    line 1
                    line 2
            runcmd:
              - foo
              - bar
            
        """.trimIndent()
    }

    @Test
    fun testShellScriptToCloudInit() {
        val script = ShellScript()

        /** inline sources are directly included in the rendered script */
        script.addLibrary(StorageLibrary)
        script.addLibrary(DockerLibrary)

        /** lib sources are written to a file and sourced into the generated script */
        script.addLibrary(AptLibrary)

        script.addCommand(AptLibrary.UpdateRepositories())
        script.addCommand(AptLibrary.InstallPackage("jq"))
        script.addCommand(DockerLibrary.InstallDebian())

        println(script.toCloudInit("rsa-key", "ed25519-key").render())
    }
}
