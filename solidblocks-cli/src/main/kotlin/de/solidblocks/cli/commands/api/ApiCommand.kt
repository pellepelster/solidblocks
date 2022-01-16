package de.solidblocks.cli.commands.api

import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.ApplicationContext
import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.model.generateRsaKeyPair

class ApiCommand : BaseCloudDbCommand(name = "api", help = "start cloud api") {
    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)
        context.usersManager.ensureAdminUser("admin", "admin")


        val keyPair = generateRsaKeyPair()
        val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = 8080)
        val authApi = AuthApi(httpServer, context.cloudRepository, context.environmentRepository, context.usersManager)

        httpServer.waitForShutdown()
    }
}
