package de.solidblocks.service.vault

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Capability
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.base.ServiceReference
import de.solidblocks.service.vault.config.RaftStorage
import de.solidblocks.service.vault.config.Tcp
import de.solidblocks.service.vault.config.Vault
import de.solidblocks.vault.InitializingVaultManager
import de.solidblocks.vault.VaultCredentials
import de.solidblocks.vault.VaultManager
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.vault.VaultException
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import kotlin.io.path.writeBytes

@JsonIgnoreProperties(ignoreUnknown = true)
data class RaftAutopilotResponse(val healthy: Boolean, val leader: String)

class VaultServiceManager(val reference: ServiceReference, val storageDir: String, val solidblocksVaultManager: VaultManager) {

    private val DOCKER_IMAGE = "solidblocks-service-vault"

    private val USER_ID = 4000

    private val GROUP_ID = 4000

    private val SERVICE_LABEL_KEY = "service"

    private val bindPort = Ports.Binding.bindPort(8200)

    private val logger = KotlinLogging.logger {}

    private val credentialsPath: String get() = "${reference.service}/vault_credentials"

    val objectMapper = jacksonObjectMapper()

    val dockerClient: DockerClient

    val config = RetryConfig.custom<Boolean>().retryOnResult { it == false }.maxAttempts(20).waitDuration(Duration.ofSeconds(1))

    val retry = Retry.of("healthcheck", config.build())

    init {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
        dockerClient = DockerClientImpl.getInstance(config, ZerodepDockerHttpClient.Builder().dockerHost(URI.create("unix:///var/run/docker.sock")).build())
    }

    val vaultAddress: String
        get() = "http://localhost:${bindPort.hostPortSpec}"

    fun loadCredentials(): VaultCredentials? {
        val credentials = solidblocksVaultManager.loadKv(credentialsPath, VaultCredentials::class.java)

        if (credentials == null) {
            logger.error { "failed to load vault credentials from '$credentialsPath'" }
        }

        return credentials
    }

    fun start(): Boolean {
        logger.info { "starting vault service '$DOCKER_IMAGE'" }

        val storage = File(storageDir)
        if (!storage.exists()) {
            logger.error { "storage dir '$storageDir' does not exists" }
            return false
        }

        if (!storage.isDirectory) {
            logger.error { "storage dir '$storageDir' is not a directory" }
            return false
        }

        val uid = Files.getAttribute(storage.toPath(), "unix:uid")
        val gid = Files.getAttribute(storage.toPath(), "unix:gid")

        if (uid != USER_ID && gid != GROUP_ID) {
            // logger.error { "storage dir '${storageDir}' needs to have user id ${USER_ID} and/or group id ${GROUP_ID}" }
            // return false
        }

        val permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"))
        val tempDir = Files.createTempDirectory("${reference.cloud}-${reference.environment}-${reference.service}")

        val vaultConfig = Vault(
            api_addr = "http://127.0.0.1:8201",
            cluster_addr = "http://127.0.0.1:8200",
            listener = Tcp("0.0.0.0:8200", true), storage = RaftStorage("/storage/local", "node1")
        )

        val vaultConfigFile = Path.of(tempDir.toString(), "vault-config.json")
        logger.info { "writing vault config to '$vaultConfigFile" }

        vaultConfigFile.writeBytes(objectMapper.writeValueAsBytes(vaultConfig))

        val result = dockerClient.createContainerCmd(DOCKER_IMAGE)
            .withExposedPorts(ExposedPort(8200))
            .withLabels(mapOf(SERVICE_LABEL_KEY to "vault"))
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withCapAdd(Capability.IPC_LOCK)
                    .withPortBindings(PortBinding(bindPort, ExposedPort(8200)))
                    .withBinds(Bind(storageDir, Volume("/storage/local")), Bind(vaultConfigFile.toString(), Volume("/solidblocks/config/vault.json")))
            ).exec()
        dockerClient.startContainerCmd(result.id).exec()

        if (!waitForReady()) return false

        val initializingVaultManager = InitializingVaultManager(vaultAddress)

        if (!initializingVaultManager.isInitialized()) {
            val credentials = initializingVaultManager.initialize()
            if (!solidblocksVaultManager.storeKv(credentialsPath, credentials)) {
                logger.error { "storing vault credentials at '$credentialsPath' failed" }
                return false
            }
        }

        if (initializingVaultManager.isInitialized() && !solidblocksVaultManager.hasKv(credentialsPath)) {
            logger.error { "vault is initialized but no credentials found for unsealing at '$credentialsPath'" }
            return false
        }

        val credentials = loadCredentials() ?: return false

        if (!initializingVaultManager.unseal(credentials)) {
            logger.error { "unsealing vault failed with credentials from '$credentialsPath'" }
            return false
        }

        if (!waitForRaftToSettle(credentials.rootToken)) return false

        return true
    }

    private fun waitForReady(): Boolean {

        val result = retry.executeCallable {
            logger.info { "waiting for vault to become ready" }
            val vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(vaultAddress)))
            val health = vaultTemplate.opsForSys().health()
            health.isSealed || !health.isInitialized
        }

        if (!result) {
            logger.error { "vault is not ready" }
        } else {
            logger.info { "vault is ready" }
        }

        return result
    }

    private fun waitForRaftToSettle(token: String): Boolean {
        val vaultTemplate = VaultTemplate(VaultEndpoint.from(URI.create(vaultAddress)), TokenAuthentication(token))

        val result = retry.executeCallable {
            logger.info { "waiting for raft to settle" }
            try {
                val response = vaultTemplate.read("/sys/storage/raft/autopilot/state", RaftAutopilotResponse::class.java)

                if (response == null || response.data == null) {
                    return@executeCallable false
                }

                return@executeCallable response.data!!.healthy
            } catch (e: VaultException) {
                return@executeCallable false
            }
        }

        if (!result) {
            logger.error { "vault raft is not settled" }
        } else {
            logger.info { "vault raft is settled" }
        }

        return result
    }

    private fun serviceContainers() = dockerClient.listContainersCmd().exec().filter { it.labels[SERVICE_LABEL_KEY] == "vault" }

    fun stop() {
        serviceContainers().forEach {
            logger.info { "stopping container '${it.id}'" }
            dockerClient.stopContainerCmd(it.id).exec()
        }
    }

    fun isRunning(): Boolean = serviceContainers().isNotEmpty()

    fun backup(): Boolean {
        val credentials = loadCredentials() ?: return false

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$vaultAddress/v1/sys/storage/raft/snapshot")
            .addHeader("X-Vault-Token", credentials.rootToken)
            .build()

        val bytes = client.newCall(request).execute().use {
            it.body!!.bytes()
        }

        bytes.toString()

        return true
    }
}
