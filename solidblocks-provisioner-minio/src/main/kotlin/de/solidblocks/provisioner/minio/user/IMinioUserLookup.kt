package de.solidblocks.provisioner.minio.user

import de.solidblocks.core.IResourceLookup

interface IMinioUserLookup : IResourceLookup<MinioUserRuntime> {
    fun name(): String
}
