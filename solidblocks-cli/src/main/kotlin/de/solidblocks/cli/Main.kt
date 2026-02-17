package de.solidblocks.cli


import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.cloud.CloudApplyCommand
import de.solidblocks.cli.cloud.CloudCommand
import de.solidblocks.cli.cloud.CloudHelpCommand
import de.solidblocks.cli.cloud.CloudPlanCommand
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.docs.DocsCommand
import de.solidblocks.cli.docs.ansible.AnsibleCommand
import de.solidblocks.cli.github.GithubCommand
import de.solidblocks.cli.github.GithubRegistryCleanCommand
import de.solidblocks.cli.hetzner.HetznerCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgRotateCommand
import de.solidblocks.cli.hetzner.nuke.HetznerNukeCommand
import de.solidblocks.cli.terraform.*
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization
import java.io.File
import java.security.Security
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

class LoggerInit

class BouncyCastleFeature : Feature {
    public override fun afterRegistration(access: Feature.AfterRegistrationAccess?) {
        RuntimeClassInitialization.initializeAtBuildTime("org.bouncycastle")

        Security.addProvider(
            BouncyCastleProvider()
        )
    }
}

fun main1(args: Array<String>) {
    val key = SSHKeyUtils.ED25519.loadFromOpenSSH(File("/home/pelle/git/solidblocks/solidblocks-ssh/src/test/resources/test_ed25519.key").readText())
    println("Key: $key")
    SSHClient("65.108.58.98", key!!).command("whoami")
}

fun main(args: Array<String>) {
    LogManager.getLogManager()
        .readConfiguration(LoggerInit::class.java.getResourceAsStream("/logging.properties"))
    val logger = Logger.getLogger(LoggerInit::class.java.getName())
    logger.log(Level.INFO, "starting CLI")

    val root = BlcksCommand()

    CloudCommand().also {
        root.subcommands(it)
        it.subcommands(CloudApplyCommand(), CloudPlanCommand(), CloudHelpCommand())
    }

    HetznerCommand().also {
        root.subcommands(it)
        it.subcommands(HetznerNukeCommand())
        it.subcommands(HetznerAsgCommand().subcommands(HetznerAsgRotateCommand()))
    }

    GithubCommand().also {
        root.subcommands(it)
        it.subcommands(GithubRegistryCleanCommand())
    }

    DocsCommand().also {
        root.subcommands(it)
        it.subcommands(AnsibleCommand())
    }

    TerraformCommand().also {
        root.subcommands(it)
        it.subcommands(
            BackendsCommand(TYPE.TERRAFORM).also { it.subcommands(BackendsS3Command(TYPE.TERRAFORM)) },
        )
    }

    TofuCommand().also {
        root.subcommands(it)
        it.subcommands(
            BackendsCommand(TYPE.TOFU).also { it.subcommands(BackendsS3Command(TYPE.TOFU)) },
        )
    }

    root.main(args)
}
