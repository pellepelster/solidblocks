package de.solidblocks.webs3docker.test

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.infra.test.SolidblocksTestContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until

open class BaseIntegrationTest {

  var imageTag = UUID.randomUUID().toString()

  lateinit var docker: DockerClient
  lateinit var s3Buckets: List<S3Bucket>
  lateinit var s3Host: String
  lateinit var dockerHostPrivate: String
  var dockerHostPublic: String? = null
  lateinit var dockerRwUsers: List<DockerUser>

  fun init(context: SolidblocksTestContext, dockerEnable: Boolean, dockerPublicEnable: Boolean) {
    val baseTerraform = context.terraform(Path.of("./src/test/resources/terraform/base"))
    baseTerraform.init()
    baseTerraform.apply()
    val baseOutput = baseTerraform.output()
    val privateKey = baseOutput.getString("private_key")

    val terraform = context.terraform(Path.of("./src/test/resources/terraform/web-s3-docker"))
    terraform.addVariable("test_id", baseOutput.getString("test_id"))
    terraform.addVariable("docker_enable", dockerEnable)
    terraform.addVariable("docker_public_enable", dockerPublicEnable)
    terraform.addVariable("disable_volume_delete_protection", true)

    terraform.init()
    terraform.apply()
    val output = terraform.output()

    val ipv4Address = output.getString("ipv4_address")

    val host = context.host(ipv4Address)
    await atMost (30.seconds.toJavaDuration()) until { host.portIsOpen(22) }

    val cloudInit = context.cloudInit(ipv4Address, privateKey)
    cloudInit.printOutputLogOnTestFailure()

    s3Host = output.getString("s3_host")
    println("s3Host: $s3Host")
    s3Buckets = output.getList("s3_buckets", S3Bucket::class)

    dockerHostPrivate = output.getString("docker_host_private")
    println("dockerHostPrivate: $dockerHostPrivate")

    dockerHostPublic = output.getOptionalString("docker_host_public")
    println("dockerHostPublic: $dockerHostPublic")

    waitForUrl("https://$dockerHostPrivate")
    waitForUrl("https://$s3Host")

    println("pushing docker image '$dockerHostPrivate/alpine'")
    dockerRwUsers = output.getList("docker_rw_users", DockerUser::class)
    docker = createDockerClient()
    docker
        .pullImageCmd("alpine")
        .withTag("latest")
        .exec(PullImageResultCallback())
        .awaitCompletion()

    docker.tagImageCmd("alpine:latest", "$dockerHostPrivate/alpine", imageTag).exec()

    val dockerRwUser = dockerRwUsers.first()

    val rwAuth =
        AuthConfig().withUsername(dockerRwUser.username).withPassword(dockerRwUser.password)
    println("pushing with user '${rwAuth.username}' and password '${rwAuth.password}'")

    docker
        .pushImageCmd("$dockerHostPrivate/alpine")
        .withTag(imageTag)
        .withAuthConfig(rwAuth)
        .exec(PushImageResultCallback())
        .awaitCompletion()
  }

  fun createDockerClient(): DockerClient {
    val config: DefaultDockerClientConfig.Builder =
        DefaultDockerClientConfig.createDefaultConfigBuilder()

    val httpClient = ZerodepDockerHttpClient.Builder()
    httpClient.dockerHost(URI.create("unix:///var/run/docker.sock"))

    val dockerClient: DockerClient =
        DockerClientBuilder.getInstance(config.build())
            .withDockerHttpClient(httpClient.build())
            .build()
    return dockerClient
  }
}

private fun waitForUrl(url: String) {
  await.atMost(Duration.ofMinutes(3)).pollInterval(Duration.ofSeconds(5)).until {
    try {
      println("waiting for '$url'")
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.apply {
        requestMethod = "GET"
        connectTimeout = 5000
        readTimeout = 5000
      }

      connection.disconnect()

      if (connection.responseCode in 101..<600) {
        true
      } else {
        false
      }
    } catch (e: Exception) {
      false
    }
  }
}
