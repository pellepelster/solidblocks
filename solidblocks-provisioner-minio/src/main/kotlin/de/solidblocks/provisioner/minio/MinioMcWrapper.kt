package de.solidblocks.provisioner.minio

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.CommandExecutor
import de.solidblocks.base.CommandResult
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes

class MinioMcWrapper(val minioUrl: String, val accessKey: String, val secretKey: String) {

    val instanceId = UUID.randomUUID()

    val objectMapper = jacksonObjectMapper()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MinioUser(
        val status: String,
        val accessKey: String,
        val userStatus: String,
        @JsonProperty("policyName")
        private val rawPolicyName: String?
    ) {

        val policies: List<String>
            get() = rawPolicyName?.split(",").orEmpty()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MinioGenericResult(val status: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MinioPolicy(val status: String, val policy: String, val isGroup: Boolean)

    data class Statement(
        @JsonProperty("Effect")
        val effect: String = "Allow",
        @JsonProperty("Sid")
        val sid: String = "",
        @JsonProperty("Action")
        val action: List<String> = emptyList(),
        @JsonProperty("Resource")
        val resource: List<String> = emptyList()
    )

    data class Policy(
        @JsonProperty("Version")
        val version: String = "2012-10-17",
        @JsonProperty("Statement")
        val statement: List<Statement> = listOf()
    )

    fun addPolicy(name: String, policy: Policy): Boolean {

        val policyFile = Files.createTempFile("minio_wrapper", "policy")
        policyFile.writeBytes(objectMapper.writeValueAsBytes(policy))

        val result = runMc("admin", "policy", "add", "ALIAS", name, policyFile.toString(), "--json")

        policyFile.deleteIfExists()

        return objectMapper.readValue(result.stdout, MinioGenericResult::class.java).status == "success"
    }

    fun addUser(accessKey: String, secretKey: String): Boolean {
        val result = runMc("admin", "user", "add", "ALIAS", accessKey, secretKey, "--json")
        return objectMapper.readValue(result.stdout, MinioGenericResult::class.java).status == "success"
    }

    fun assignPolicy(policyName: String, userName: String): Boolean {
        val result = runMc("admin", "policy", "set", "ALIAS", policyName, "user=$userName", "--json")
        return objectMapper.readValue(result.stdout, MinioGenericResult::class.java).status == "success"
    }

    fun runMc(vararg command: String): CommandResult {

        val file = File("/tmp/mc_$instanceId")
        FileFromClasspath.ensureFile("/mc", file)

        val alias = "temp"
        val uri = URI.create(minioUrl)
        val url = "${uri.scheme}://$accessKey:$secretKey@${uri.host}:${uri.port}/${uri.path}"

        val result = CommandExecutor().run(
            environment = mapOf(
                "MC_HOST_$alias" to url
            ),
            command = listOf(file.toString()) + command.map {
                if (it == "ALIAS") alias
                else it
            }
        )

        if (result.error) {
            throw RuntimeException(result.stderr)
        }

        return result
    }

    fun listPolicies(): List<MinioPolicy> {
        val result = runMc("admin", "policy", "list", "ALIAS", "--json")

        return result.stdout.byteInputStream().let {
            objectMapper.readValues(JsonFactory().createParser(it), MinioPolicy::class.java)
        }.iterator().asSequence().toList()
    }

    fun listUsers(): List<MinioUser> {
        val result = runMc("admin", "user", "list", "ALIAS", "--json")

        return result.stdout.byteInputStream().let {
            objectMapper.readValues(JsonFactory().createParser(it), MinioUser::class.java)
        }.iterator().asSequence().toList()
    }
}
