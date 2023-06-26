package de.solidblocks.rds.postgresql.test

import de.solidblocks.rds.postgresql.test.extensions.*
import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import nl.altindag.ssl.util.PemUtils
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.GenericContainer
import java.util.*


@ExtendWith(RdsTestBedExtension::class)
class RdsPostgresqlMinioBackupIntegrationTest {

    companion object {
        const val backupHost = "minio"
        const val bucket = "database1-backup"
        const val accessKey = "database1-user1"
        const val secretKey = "ccbaa67e-cf26-432f-a11f-0c9e72abccf8"
        const val database = "database1"
        const val databaseUser = "user1"
        const val databasePassword = "password1"

        val minioCertificatePrivateBase64 =
            Base64.getEncoder()
                .encodeToString(
                    RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/minio.key.pem").readBytes()
                )
        val minioCertificatePublicBase64 =
            Base64.getEncoder()
                .encodeToString(
                    RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/minio.pem").readBytes()
                )
        val caPublicBase64 =
            Base64.getEncoder()
                .encodeToString(RdsPostgresqlMinioBackupIntegrationTest::class.java.getResource("/ca.pem").readBytes())


        val s3BackupEnv = mapOf(
            "DB_BACKUP_S3" to "1",
            "DB_BACKUP_S3_CA_PUBLIC_KEY" to caPublicBase64,
            "DB_BACKUP_S3_HOST" to backupHost,
            "DB_BACKUP_S3_BUCKET" to bucket,
            "DB_BACKUP_S3_ACCESS_KEY" to accessKey,
            "DB_BACKUP_S3_SECRET_KEY" to secretKey,
            "DB_BACKUP_S3_URI_STYLE" to "path"
        )
    }


    @Test
    fun testDatabaseKeepsDataBetweenRestarts(rdsTestBed: RdsTestBed) {

        rdsTestBed.createAndStartMinioContainer()

        val dataDir = initWorldReadableTempDir()
        val container = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, dataDir
        )

        with(rdsTestBed.logConsumer) {
            // on first start instance should be initialized and an initial backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }

        val username = UUID.randomUUID().toString()

        container.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }

        container.stop()
        rdsTestBed.logConsumer.clear()
        container.start()

        with(rdsTestBed.logConsumer) {
            // on second start with persistent storage no initializing ord backup should be executed
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is not empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        container.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }
    }

    @Test
    fun testRestoreDatabaseFromFullBackup(rdsTestBed: RdsTestBed) {

        val minioContainer = rdsTestBed.createAndStartMinioContainer()
        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }


        val username = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }

        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val minioClient = createMinioClient(minioContainer)

        val objects =
            minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build()).map { it.get() }
                .filter { it.objectName().endsWith(".gz") }

        assertThat(objects).hasSizeGreaterThan(10)

        objects.forEach {
            val obj = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).`object`(it.objectName()).length(8).build()
            )
            assertThat(obj.readAllBytes().toHex()).isEqualTo(COMPRESSED_FILE_HEADER)
        }

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }
    }

    @Test
    fun testRestoreDatabaseFromEncryptedBackup(rdsTestBed: RdsTestBed) {
        val minioContainer = rdsTestBed.createAndStartMinioContainer()
        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_ENCRYPTION_PASSPHRASE" to "yolo123"), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }

        val username = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username)
            it.assertHasUserWithName(username)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val minioClient = createMinioClient(minioContainer)

        val objects =
            minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build()).map { it.get() }
                .filter { it.objectName().endsWith(".gz") }

        assertThat(objects).hasSizeGreaterThan(10)

        objects.forEach {
            val obj = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).`object`(it.objectName()).length(8).build()
            )
            assertThat(obj.readAllBytes().toHex()).isEqualTo(ENCRYPTED_FILE_HEADER)
        }

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv + mapOf("DB_BACKUP_ENCRYPTION_PASSPHRASE" to "yolo123"), initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username)
        }
    }

    private fun createMinioClient(minioContainer: GenericContainer<out GenericContainer<*>>): MinioClient {
        val certificates = PemUtils.parseCertificate(String(Base64.getDecoder().decode(caPublicBase64)))

        val clientCertificates: HandshakeCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificates[0])
            .build()

        val client: OkHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()

        return MinioClient.builder()
            .httpClient(client)
            .endpoint("https://localhost:${minioContainer.getMappedPort(443)}")
            .credentials(accessKey, secretKey)
            .build()
    }

    @Test
    fun testRestoreDatabaseFromIncrementalBackup(rdsTestBed: RdsTestBed) {

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }

        val username1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {
            it.waitForReady()
            it.createUserTable()
            it.insertUser(username1)
            it.assertHasUserWithName(username1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val username2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(username2)
            it.assertHasUserWithName(username2)
        }

        postgresContainer1.execInContainer("/rds/bin/backup-incr.sh")

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username1)
            it.assertHasUserWithName(username2)
        }
    }

    @Test
    fun testRestoreDatabaseFromDifferentialBackup(rdsTestBed: RdsTestBed) {

        rdsTestBed.createAndStartMinioContainer()

        val postgresContainer1 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] executing initial backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            waitForLogLine("database system is ready to accept connections")
        }


        val username1 = UUID.randomUUID().toString()

        postgresContainer1.createJdbi().also {

            it.waitForReady()
            it.createUserTable()
            it.insertUser(username1)
            it.assertHasUserWithName(username1)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-full.sh")

        val username2 = UUID.randomUUID().toString()
        postgresContainer1.createJdbi().also {
            it.insertUser(username2)
            it.assertHasUserWithName(username2)
        }


        postgresContainer1.execInContainer("/rds/bin/backup-diff.sh")

        postgresContainer1.stop()
        rdsTestBed.logConsumer.clear()

        val postgresContainer2 = rdsTestBed.createAndStartPostgresContainer(
            s3BackupEnv, initWorldReadableTempDir()
        )

        with(rdsTestBed.logConsumer) {
            waitForLogLine("[solidblocks-rds-postgresql] provisioning completed")
            assertHasLogLine("[solidblocks-rds-postgresql] data dir is empty")
            assertHasNoLogLine("[solidblocks-rds-postgresql] initializing database instance")
            assertHasLogLine("[solidblocks-rds-postgresql] restoring database from backup")
            assertHasNoLogLine("[solidblocks-rds-postgresql] executing initial backup")
            waitForLogLine("database system is ready to accept connections")
        }

        postgresContainer2.createJdbi().also {
            it.waitForReady()
            it.assertHasUserWithName(username1)
            it.assertHasUserWithName(username2)
        }
    }
}
