package de.solidblocks.cli.commands.api

import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.ApplicationContext
import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.auth.api.AuthApi
import de.solidblocks.cloud.clouds.api.CloudsApi
import de.solidblocks.cloud.model.generateRsaKeyPair
import de.solidblocks.cloud.tenants.api.TenantsApi

class ApiCommand : BaseCloudDbCommand(name = "api", help = "start cloud api") {
    override fun run() {
        val context = ApplicationContext(solidblocksDatabaseUrl)
        context.managers.users.ensureAdminUser("admin", "admin")

        val keyPair = generateRsaKeyPair()
        val httpServer = CloudApiHttpServer(privateKey = keyPair.first, publicKey = keyPair.second, port = 8080)
        val authApi = AuthApi(httpServer, context.repositories.clouds, context.repositories.environments, context.managers.users)
        val cloudsApi = CloudsApi(httpServer, context.managers.clouds, context.managers.environments)
        val tenantsApi = TenantsApi(httpServer, context.managers.tenants)

        httpServer.waitForShutdown()
    }
}
