package de.solidblocks.shell.test

import io.minio.MinioClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class S3Bucket(
    val name: String,
    val owner_key_id: String,
    val owner_secret_key: String,
    val ro_key_id: String,
    val ro_secret_key: String,
    val rw_key_id: String,
    val rw_secret_key: String,
    val web_access_addresses: List<String>,
    val web_access_public_enable: Boolean,
)

@Serializable
@JsonIgnoreUnknownKeys
data class DockerUser(
    val username: String,
    val password: String,
)

fun S3Bucket.ownerS3Client(s3Host: String) =
    MinioClient.builder()
        .endpoint("https://$s3Host")
        .region("garage")
        .credentials(this.owner_key_id, this.owner_secret_key)
        .build()

fun S3Bucket.rwS3Client(s3Host: String) =
    MinioClient.builder()
        .endpoint("https://$s3Host")
        .region("garage")
        .credentials(this.rw_key_id, this.rw_secret_key)
        .build()

fun S3Bucket.roS3Client(s3Host: String) =
    MinioClient.builder()
        .endpoint("https://$s3Host")
        .region("garage")
        .credentials(this.ro_key_id, this.ro_secret_key)
        .build()
