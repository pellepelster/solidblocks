package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.compute.UserDataDataSource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import de.solidblocks.provisioner.Provisioner
import freemarker.template.Configuration
import freemarker.template.Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX
import freemarker.template.TemplateExceptionHandler
import org.springframework.stereotype.Component
import java.io.StringWriter

@Component
class UserDataResourceLookupProvider(val provisioner: Provisioner) : IResourceLookupProvider<UserDataDataSource, String> {

    override fun lookup(datasource: UserDataDataSource): Result<String> {

        val cfg = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)

        cfg.setClassForTemplateLoading(this::class.java, "/")
        cfg.interpolationSyntax = SQUARE_BRACKET_INTERPOLATION_SYNTAX
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER

        val input = HashMap<String, String>()

        val lookups = datasource.variables.map {
            it.key to provisioner.lookup(it.value)
        }

        if (lookups.any { it.second.isEmptyOrFailed() }) {
            return lookups.map { it.second }.reduceResults() as Result<String>
        }

        lookups.forEach {
            input[it.first] = it.second.result!!
        }

        val template = cfg.getTemplate(datasource.resourceFile)

        val sw = StringWriter()
        template.process(input, sw)

        return Result(datasource, sw.toString())
    }

    override fun getLookupType(): Class<UserDataDataSource> {
        return UserDataDataSource::class.java
    }
}
