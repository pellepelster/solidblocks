package de.solidblocks.provisioner.hetzner.cloud.server

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.InfrastructureProvisioner
import de.solidblocks.core.IResourceLookup
import de.solidblocks.core.Result
import freemarker.template.Configuration
import freemarker.template.Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX
import freemarker.template.TemplateExceptionHandler
import java.io.StringWriter

class UserDataResourceLookupProvider(val provisioner: InfrastructureProvisioner) :
    IResourceLookupProvider<UserData, UserDataRuntime> {

    val cfg = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)

    init {
        cfg.setClassForTemplateLoading(this::class.java, "/")
        cfg.interpolationSyntax = SQUARE_BRACKET_INTERPOLATION_SYNTAX
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
    }

    override fun lookup(userData: UserData): Result<UserDataRuntime> {

        val staticInput = resolveVariables(userData.staticVariables)
        val ephemeraInput = resolveVariables(userData.ephemeralVariables)

        if (staticInput == null || ephemeraInput == null) {
            return Result.failedResult()
        }

        val staticUserData = StringWriter().let {
            val template = cfg.getTemplate(userData.resourceFile)
            template.process(staticInput + userData.ephemeralVariables.map { it.key to "static" }.toMap(), it)
            it.toString()
        }

        val ephemeraUserData = StringWriter().let {
            val template = cfg.getTemplate(userData.resourceFile)
            template.process(staticInput + ephemeraInput, it)
            it.toString()
        }

        return Result(UserDataRuntime(staticUserData, ephemeraUserData))
    }

    private fun resolveVariables(variables: HashMap<String, IResourceLookup<String>>): Map<String, String>? {

        val lookups = variables.map {
            it.key to provisioner.lookup(it.value)
        }

        if (lookups.any { it.second.isEmptyOrFailed() }) {
            return null
        }

        return lookups.associate { (key, value) ->
            key to value.result!!
        }
    }

    override val lookupType = UserData::class.java
}
