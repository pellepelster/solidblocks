package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.setPosixFilePermissions
import kotlin.system.exitProcess

@Component
class CloudSshConfigCommand :
    CliktCommand(name = "ssh-config", help = "create ssh config") {

    val name: String by option(help = "name of the cloud").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {

        SpringContextUtil.callBeanAndShutdown(CloudConfigurationManager::class.java) {

            if (!it.hasTenant(name)) {
                logger.error { "cloud '$name' not found" }
                exitProcess(1)
            }

            val cloud = it.getTenant(name)

            val basePath = Path(System.getProperty("user.home"), ".solidblocks", name)

            if (!basePath.toFile().exists() && !basePath.toFile().mkdirs()) {
                logger.error { "creating dir '${basePath.toAbsolutePath()}' failed" }
                exitProcess(1)
            }

            val privateKeyFile = File(basePath.toFile(), "${name}_key")
            privateKeyFile.writeText(cloud.sshConfig.sshPrivateKey)
            privateKeyFile.toPath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

            val knownHostsFile = File(basePath.toFile(), "${name}_known_hosts")
            knownHostsFile.writeText("vault-1.$name.${cloud.solidblocksConfig.domain} ${cloud.sshConfig.sshIdentityPublicKey}")

            knownHostsFile.toPath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

            File(basePath.toFile(), "${name}_key.pub").writeText(cloud.sshConfig.sshPublicKey)
            File(basePath.toFile(), "ssh_config").writeText(
                sshConfig(
                    privateKeyFile.absolutePath,
                    knownHostsFile.absolutePath
                )
            )
        }
    }

    fun sshConfig(privateKeyFile: String, knownHostsFile: String): String {
        return """
            Host *
                User root
                IdentitiesOnly yes
                IdentityFile $privateKeyFile
                UserKnownHostsFile $knownHostsFile
        """.trimIndent()
    }
}
