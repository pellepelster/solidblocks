package de.solidblocks.cloud.configuration.model

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.configuration.*
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
        List<
            ProviderRegistration<
                out ProviderConfiguration,
                out ProviderRuntime,
                out ProviderConfigurationManager<*, *>,
            >,
        >,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
) : ConfigurationFactory<CloudConfiguration> {

  val name =
      StringKeyword(
          "name",
          KeywordHelp(
              "TODO",
              """
              Unique name for the cloud deployment. Can be up to 63 characters long and must adhere to [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name. If none is provided the name of the file containing the configuration will be used.
              """
                  .trimIndent(),
          ),
      )

  val rootDomain =
      StringKeyword(
          "root_domain",
          KeywordHelp(
              "TODO",
              "TODO",
          ),
      )

  val providers =
      PolymorphicListKeyword(
          "providers",
          providerRegistrations.associate { it.type to it.createConfigurationFactory() }
              as Map<String, PolymorphicConfigurationFactory<ProviderConfiguration>>,
          KeywordHelp("TODO", "TODO"),
      )

  val services =
      PolymorphicListKeyword<ServiceConfiguration>(
          "services",
          serviceRegistrations.associate { it.type to it.createConfigurationFactory() }
              as Map<String, PolymorphicConfigurationFactory<ServiceConfiguration>>,
          KeywordHelp("TODO", "TODO"),
      )

  override val help: ConfigurationHelp =
      ConfigurationHelp(
          "Cloud",
          """
          A Solidblocks cloud is defined in YAML based configuration file with the following format

          """
              .trimIndent(),
      )

  override val keywords = listOf<Keyword<*>>(name, providers, services)

  override fun parse(yaml: YamlNode): Result<CloudConfiguration> {
    val name =
        when (val name = name.parse(yaml)) {
          is Error<*> -> return Error(name.error)
          is Success<String> -> name.data
        }

    val rootDomain =
        when (val rootDomain = rootDomain.parse(yaml)) {
          is Error<*> -> return Error(rootDomain.error)
          is Success<String> -> rootDomain.data
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

    return Success(CloudConfiguration(name, rootDomain, providers, services))
  }
}
