package de.solidblocks.provisioner.minio.bucket

import de.solidblocks.core.IResourceLookup

interface IMinioBucketLookup : IResourceLookup<MinioBucketRuntime> {
    fun name(): String
}
