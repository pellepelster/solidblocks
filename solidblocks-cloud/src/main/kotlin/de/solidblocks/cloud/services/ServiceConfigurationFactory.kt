package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.BooleanKeyword
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.OptionalStringMapKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.configuration.constraints
import de.solidblocks.cloud.configuration.default
import de.solidblocks.cloud.interpolation.validateInterpolatedString
import de.solidblocks.cloud.services.InstanceConfigurationFactory.parseInstanceConfig
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.result

object ServiceConfigurationFactory {

    val SERVICE_FLOATING_IP_KEYWORD =
        BooleanKeyword(
            "use_floating_ip",
            KeywordHelp("use floating ip for public server access"),
        ).default(false)

    val SERVICE_NAME_KEYWORD =
        StringKeyword(
            "name",
            KeywordHelp(
                "Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.",
            ),
        ).constraints(RFC_1123_NAME)

    val SERVICE_ENVIRONMENT_VARS_KEYWORD =
        OptionalStringMapKeyword(
            "environment_vars",
            KeywordHelp(
                "Environment variables that should be set for the service, will be merged with the globally configured environment variables",
            ),
        )

    val keywords =
        listOf<Keyword<*>>(
            SERVICE_NAME_KEYWORD,
            SERVICE_ENVIRONMENT_VARS_KEYWORD,
            SERVICE_FLOATING_IP_KEYWORD,
        ) + InstanceConfigurationFactory.keywords

    @Suppress("UNCHECKED_CAST")
    fun Map<String, String>.validateInterpolatedStrings(serviceName: String): Result<Unit> {
        val errors = this.entries.associate {
            it.key to validateInterpolatedString(it.value)
        }.filter { it.value != null } as Map<String, String>

        return if (errors.isNotEmpty()) {
            Error(
                "invalid interpolation for environment in service '$serviceName': ${
                    errors.entries.joinToString(", ") {
                        "${it.key} (${it.value})"
                    }
                }",
            )
        } else {
            Success(Unit)
        }
    }

    fun YamlNode.parseServiceCommonConfig(): Result<ServiceCommonConfig> {
        val yaml = this
        return result {
            val name = SERVICE_NAME_KEYWORD.parse(yaml).bind()
            val instance = yaml.parseInstanceConfig().bind()
            val environmentVars = SERVICE_ENVIRONMENT_VARS_KEYWORD.parse(yaml).bind() ?: emptyMap()
            val floatingIp = SERVICE_FLOATING_IP_KEYWORD.parse(yaml).bind()

            environmentVars.validateInterpolatedStrings(name).bind()

            ServiceCommonConfig(name, floatingIp, environmentVars, instance)
        }
    }
}
