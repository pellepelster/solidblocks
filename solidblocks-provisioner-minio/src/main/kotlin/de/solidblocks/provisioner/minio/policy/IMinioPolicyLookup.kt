package de.solidblocks.provisioner.minio.policy

import de.solidblocks.core.IResourceLookup

interface IMinioPolicyLookup : IResourceLookup<MinioPolicyRuntime> {
    fun name(): String
}
