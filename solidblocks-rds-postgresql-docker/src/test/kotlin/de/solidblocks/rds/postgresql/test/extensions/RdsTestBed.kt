package de.solidblocks.rds.postgresql.test.extensions

import de.solidblocks.rds.postgresql.test.TestConstants
import de.solidblocks.rds.postgresql.test.TestConstants.DATABASE
import de.solidblocks.rds.postgresql.test.TestConstants.DATABASE_PASSWORD
import de.solidblocks.rds.postgresql.test.TestConstants.DATABASE_USER
import java.io.File
import java.time.Duration
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.PullPolicy

class RdsTestBed {

  private val network = Network.newNetwork()

  private val containers = mutableListOf<GenericContainer<out GenericContainer<*>>>()

  val logConsumer = TestContainersLogConsumer(Slf4jLogConsumer(logger))

  private fun createContainer(dockerImageName: String): GenericContainer<out GenericContainer<*>> {
    val container = GenericContainer(dockerImageName)
    containers.add(container)
    return container
  }

  fun createAndStartPostgresContainer(
      postgresVersion: Int,
      environment: Map<String, String>,
      storageDir: File,
      password: String = DATABASE_PASSWORD,
      customizer: (input: GenericContainer<out GenericContainer<*>>) -> Unit = {},
  ) =
      createAndStartPostgresContainer(
          "ghcr.io/pellepelster/solidblocks-rds-postgresql:$postgresVersion-${
                System.getenv("VERSION") ?: "snapshot"
            }-rc",
          environment,
          storageDir,
          password,
          customizer,
      )

  fun createAndStartPostgresContainer(
      imageName: String,
      environment: Map<String, String>,
      storageDir: File,
      password: String = DATABASE_PASSWORD,
      customizer: (input: GenericContainer<out GenericContainer<*>>) -> Unit = {},
  ): GenericContainer<out GenericContainer<*>> =
      GenericContainer(imageName).also {
        it.withLogConsumer(logConsumer)
        it.withImagePullPolicy(PullPolicy.alwaysPull())
        it.withNetwork(network)
        it.withExposedPorts(5432)
        it.withFileSystemBind(storageDir.absolutePath, "/storage/data")
        it.withStartupTimeout(Duration.ofMinutes(8))
        it.withFileSystemBind(
            TestConstants::class.java.getResource("/test-dump-backup-file-headers.sh").file,
            "/test-dump-backup-file-headers.sh",
            BindMode.READ_ONLY,
        )
        it.withEnv(
            mapOf(
                "DB_INSTANCE_NAME" to DATABASE,
                "DB_DATABASE_$DATABASE" to DATABASE,
                "DB_USERNAME_$DATABASE" to DATABASE_USER,
                "DB_PASSWORD_$DATABASE" to password,
            ) + environment,
        )
        customizer.invoke(it)
        logger.info { "[test] starting postgres container" }
        containers.add(it)
        it.start()
      }

  fun clean() {
    val client = DockerClientFactory.instance().client()

    containers
        .mapNotNull { it.containerId }
        .forEach {
          try {
            logger.info { "[test] killing container '$it'" }
            client.killContainerCmd(it).withSignal("9").exec()

            logger.info { "[test] removing container '$it'" }
            client.removeContainerCmd(it).withForce(true).withRemoveVolumes(true).exec()
          } catch (e: Exception) {
            logger.info { "failed to kill container $it" }
          }
        }

    try {
      logger.info { "[test] removing network '${network.id}'" }
      network.close()
      client.removeNetworkCmd(network.id).exec()
    } catch (e: Exception) {
      logger.info { "failed to remove network ${network.id}" }
    }
  }
}
