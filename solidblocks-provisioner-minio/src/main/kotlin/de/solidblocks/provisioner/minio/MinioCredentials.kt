package de.solidblocks.provisioner.minio

import io.minio.MinioClient

data class MinioCredentials(val address: String, val accessKey: String, val secretKey: String)

fun createMinioClient(credentialProvider: () -> MinioCredentials): MinioClient {
    val credentials = credentialProvider.invoke()

    return MinioClient.builder()
        .endpoint(credentials.address)
        .credentials(credentials.accessKey, credentials.secretKey)
        .build()
}
