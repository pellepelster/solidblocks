package de.solidblocks.cloud.configuration.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.Keyword
import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.configuration.StringKeywordOptional
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import de.solidblocks.cloud.configuration.PolymorphicListKeyword
import de.solidblocks.cloud.configuration.StringConstraints.Companion.DOMAIN_NAME
import de.solidblocks.cloud.configuration.StringConstraints.Companion.RFC_1123_NAME
import de.solidblocks.cloud.configuration.StringKeyword
import de.solidblocks.cloud.documentation.model.ConfigurationHelp
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.ProviderRuntime
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

class CloudConfigurationFactory(
    providerRegistrations:
    List<ProviderRegistration<
            out ProviderConfiguration,
            out ProviderRuntime,
            out ProviderConfigurationManager<*, *>>>,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
) : ConfigurationFactory<CloudConfigurationRuntime> {

    companion object {
        val name =
            StringKeyword(
                "name",
                RFC_1123_NAME,
                KeywordHelp(
                    "Unique name for the cloud deployment. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name. If you plan to deploy multiple Solidblocks cloud configurations to a single provider account make sure the names are unique across all configuration files."
                ),
            )

        val rootDomain =
            StringKeywordOptional(
                "root_domain",
                DOMAIN_NAME,
                KeywordHelp(
                    "Root domain to use for addresses of created services, e.g. `<service_name>.<root_domain>`. If set the domain must be manageable by one of the configured providers.",
                ),
            )
    }

    @Suppress("UNCHECKED_CAST")
    val providers =
        PolymorphicListKeyword(
            "providers",
            providerRegistrations.associate { it.type to it.createConfigurationFactory() }
                    as Map<String, PolymorphicConfigurationFactory<ProviderConfiguration>>,
            KeywordHelp("Provider list, if two providers of the same type are configured, unique names must be provided. For a minimal configuration at least a SSH, secret and cloud provider is needed."),
        )

    @Suppress("UNCHECKED_CAST")
    val services =
        PolymorphicListKeyword(
            "services",
            serviceRegistrations.associate { it.type to it.createConfigurationFactory() }
                    as Map<String, PolymorphicConfigurationFactory<ServiceConfiguration>>,
            KeywordHelp("Services to create, service names must be unique across all services"),
        )

    override val help: ConfigurationHelp =
        ConfigurationHelp(
            "Configuration",
            "A Solidblocks instance can be defined using a YAML based configuration file with the following format"
        )

    override val keywords = listOf<Keyword<*>>(name, rootDomain, providers, services)

    override fun parse(yaml: YamlNode): Result<CloudConfigurationRuntime> {
        val name =
            when (val name = name.parse(yaml)) {
                is Error<*> -> return Error(name.error)
                is Success<String> -> name.data
            }

        val rootDomain =
            when (val rootDomain = rootDomain.parse(yaml)) {
                is Error<*> -> return Error(rootDomain.error)
                is Success<String?> -> rootDomain.data
            }

        val providers =
            when (val providers = providers.parse(yaml)) {
                is Error<*> -> return Error(providers.error)
                is Success<List<ProviderConfiguration>> -> providers.data
            }

        val services =
            when (val services = services.parse(yaml)) {
                is Error<*> -> return Error(services.error)
                is Success<List<ServiceConfiguration>> -> services.data
            }

        return Success(CloudConfigurationRuntime(name, rootDomain, providers, services))
    }
}
