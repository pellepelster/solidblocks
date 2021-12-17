package de.solidblocks.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.self.SolidBlocksCli

abstract class BaseCloudDbCommand(
    help: String = "",
    name: String? = null
) :
    CliktCommand(name = name, help = help) {

    val solidblocksDatabaseUrl: String
        get() {
            val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
            val dbPassword = config[SolidBlocksCli.DB_PASSWORD_KEY]
            val dbPath = config[SolidBlocksCli.DB_PATH_KEY]

            return "jdbc:derby:directory:$dbPath;create=true;dataEncryption=true;encryptionKeyLength=256;encryptionAlgorithm=AES/CBC/NoPadding;bootPassword=$dbPassword;user=solidblocks"
        }
}
