package de.solidblocks.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.self.SolidBlocksCli
import de.solidblocks.cloud.config.SolidblocksDatabase

abstract class BaseCloudDbCommand(
    help: String = "",
    name: String? = null
) :
    CliktCommand(name = name, help = help) {

    private var db: SolidblocksDatabase? = null

    fun solidblocksDatabase(): SolidblocksDatabase {

        if (db == null) {
            val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
            val dbPassword = config[SolidBlocksCli.DB_PASSWORD_KEY]
            val dbPath = config[SolidBlocksCli.DB_PATH_KEY]

            val jdbcUrl = "jdbc:derby:directory:$dbPath;create=true;dataEncryption=true;encryptionKeyLength=256;encryptionAlgorithm=AES/CBC/NoPadding;bootPassword=$dbPassword;user=solidblocks"
            db = SolidblocksDatabase(jdbcUrl)
        }

        db!!.ensureDBSchema()
        return db!!
    }
}
