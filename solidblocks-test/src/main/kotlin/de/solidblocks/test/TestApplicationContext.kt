package de.solidblocks.test

import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.provisioner.minio.MinioCredentials

class TestApplicationContext(
    jdbcUrl: String,
    val vaultAddressOverride: String? = null,
    val minioCredentialsProvider: (() -> MinioCredentials)? = null,
    development: Boolean = false
) : BaseApplicationContext(jdbcUrl, vaultAddressOverride, minioCredentialsProvider, development)
