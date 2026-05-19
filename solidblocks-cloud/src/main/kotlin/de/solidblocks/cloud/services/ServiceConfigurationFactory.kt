package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.OptionalBooleanKeyword
import de.solidblocks.cloud.configuration.OptionalStringMapKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.interpolation.validateInterpolatedString
import de.solidblocks.cloud.services.InstanceConfigurationFactory.parseInstanceConfig
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

object ServiceConfigurationFactory {

    val SERVICE_FLOATING_IP_KEYWORD =
        OptionalBooleanKeyword(
            "use_floating_ip",
            KeywordHelp("use floating ip for public server access"),
            false,
        )

    val SERVICE_NAME_KEYWORD =
        StringKeyword(
            "name",
            RFC_1123_NAME,
            KeywordHelp(
                "Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.",
            ),
        )

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
        val name =
            when (val result = SERVICE_NAME_KEYWORD.parse(this)) {
                is de.solidblocks.cloud.utils.Error<*> -> return Error(result.error)
                is Success<String> -> result.data
            }

        val instance =
            when (val result = this.parseInstanceConfig()) {
                is Error<InstanceConfig> -> return Error(result.error)
                is Success<InstanceConfig> -> result.data
            }

        val environmentVars =
            when (val result = SERVICE_ENVIRONMENT_VARS_KEYWORD.parse(this)) {
                is Error<Map<String, String>?> -> return Error(result.error)
                is Success<Map<String, String>?> -> result.data ?: emptyMap()
            }

        val floatingIp =
            when (val result = SERVICE_FLOATING_IP_KEYWORD.parse(this)) {
                is Error<Boolean> -> return Error(result.error)
                is Success<Boolean> -> result.data
            }

        when (val result = environmentVars.validateInterpolatedStrings(name)) {
            is Error<Unit> -> return Error(result.error)
            else -> {}
        }

        return Success(ServiceCommonConfig(name, floatingIp, environmentVars, instance))
    }
}
