package de.solidblocks.docker

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val yaml =
    Yaml(
        configuration =
            YamlConfiguration(
                encodeDefaults = false,
                encodingIndentationSize = 2,
            ),
    )

fun ComposeFile.toYaml(): String = yaml.encodeToString(ComposeFile.serializer(), this)

@Serializable
data class ComposeFile(
    val version: String = "3.9",
    val services: Map<String, Service> = emptyMap(),
    val networks: Map<String, NetworkDefinition> = emptyMap(),
)

@Serializable
data class Service(
    val image: String? = null,
    val command: StringOrList? = null,
    val entrypoint: StringOrList? = null,
    @SerialName("container_name") val containerName: String? = null,
    val hostname: String? = null,
    val restart: RestartPolicy? = null,
    val ports: List<PortMapping> = emptyList(),
    val expose: List<String> = emptyList(),
    val environment: Map<String, String>? = null,
    @SerialName("env_file") val envFile: StringOrList? = null,
    val volumes: List<VolumeMount> = emptyList(),
    val networks: List<String> = emptyList(),
    @SerialName("depends_on") val dependsOn: DependsOn? = null,
    val links: List<String> = emptyList(),
    val labels: Map<String, String>? = null,
    @SerialName("extra_hosts") val extraHosts: List<String> = emptyList(),
    @SerialName("dns") val dns: StringOrList? = null,
    val secrets: List<String> = emptyList(),
    val configs: List<String> = emptyList(),
    @SerialName("health_check") val healthCheck: HealthCheck? = null,
    @SerialName("logging") val logging: Logging? = null,
    @SerialName("network_mode") val networkMode: String? = null,
    val user: String? = null,
    @SerialName("working_dir") val workingDir: String? = null,
    @SerialName("read_only") val readOnly: Boolean? = null,
    val privileged: Boolean? = null,
    @SerialName("cap_add") val capAdd: List<String> = emptyList(),
    @SerialName("cap_drop") val capDrop: List<String> = emptyList(),
    @SerialName("security_opt") val securityOpt: List<String> = emptyList(),
    @SerialName("shm_size") val shmSize: String? = null,
    @SerialName("stdin_open") val stdinOpen: Boolean? = null,
    val tty: Boolean? = null,
    @SerialName("stop_signal") val stopSignal: String? = null,
    @SerialName("stop_grace_period") val stopGracePeriod: String? = null,
    val ulimits: Map<String, ULimit> = emptyMap(),
    @SerialName("sysctls") val sysctls: Map<String, String> = emptyMap(),
    val profiles: List<String> = emptyList(),
    val platform: String? = null,
    @SerialName("pull_policy") val pullPolicy: PullPolicy? = null,
)

enum class Protocol {
  tcp,
  udp,
}

@Serializable
class PortMapping(
    val target: Int,
    val published: Int? = null,
    val protocol: Protocol? = null,
    val mode: String? = null,
)

@Serializable
data class VolumeMount(
    val source: String? = null,
    val target: String,
    @SerialName("read_only") val readOnly: Boolean? = null,
    val bind: BindOptions? = null,
    val volume: VolumeOptions? = null,
)

@Serializable
data class BindOptions(
    val propagation: String? = null,
    @SerialName("create_host_path") val createHostPath: Boolean? = null,
    @SerialName("selinux") val selinux: String? = null,
)

@Serializable data class VolumeOptions(val nocopy: Boolean? = null)

@Serializable
data class NetworkDefinition(
    val driver: String? = null,
    @SerialName("driver_opts") val driverOpts: Map<String, String> = emptyMap(),
    val internal: Boolean? = null,
    val attachable: Boolean? = null,
    val labels: Map<String, String> = emptyMap(),
    val name: String? = null,
    val ipam: Ipam? = null,
)

@Serializable
data class Ipam(val driver: String? = null, val config: List<IpamConfig> = emptyList())

@Serializable data class IpamConfig(val subnet: String? = null, val gateway: String? = null)

@Serializable
sealed class DependsOn {
  @Serializable @SerialName("list") data class ServiceList(val services: List<String>) : DependsOn()

  @Serializable
  @SerialName("map")
  data class ServiceMap(val conditions: Map<String, ServiceCondition>) : DependsOn()
}

@Serializable
data class ServiceCondition(
    val condition: Condition = Condition.service_started,
    val restart: Boolean? = null,
)

@Serializable
enum class Condition {
  service_started,
  service_healthy,
  service_completed_successfully,
}

@Serializable
sealed class StringOrList {
  @Serializable @SerialName("string") data class Single(val value: String) : StringOrList()

  @Serializable @SerialName("list") data class Many(val values: List<String>) : StringOrList()
}

@Serializable
data class UpdateConfig(
    val parallelism: Int? = null,
    val delay: String? = null,
    @SerialName("failure_action") val failureAction: String? = null,
    val monitor: String? = null,
    @SerialName("max_failure_ratio") val maxFailureRatio: Double? = null,
    val order: String? = null,
)

@Serializable
data class HealthCheck(
    val test: StringOrList? = null,
    val interval: String? = null,
    val timeout: String? = null,
    val retries: Int? = null,
    @SerialName("start_period") val startPeriod: String? = null,
    val disable: Boolean? = null,
)

@Serializable
data class Logging(val driver: String? = null, val options: Map<String, String> = emptyMap())

@Serializable
enum class RestartPolicy {
  no,
  always,
  @SerialName("on-failure") on_failure,
  @SerialName("unless-stopped") unless_stopped,
}

@Serializable
enum class PullPolicy {
  always,
  never,
  missing,
  build,
}

@Serializable data class ULimit(val soft: Int? = null, val hard: Int? = null)
