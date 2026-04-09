package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.logMessage

class StringListConfigurationFactory : ConfigurationFactory<String> {

    override val help = ConfigurationHelp("TODO", "TODO")

    override val keywords = emptyList<SimpleKeyword<*>>()

    override fun parse(yaml: YamlNode): Result<String> = when (yaml) {
        is YamlScalar -> Success(yaml.content)
        else -> Error("expected a string at ${yaml.location.logMessage()}")
    }
}
