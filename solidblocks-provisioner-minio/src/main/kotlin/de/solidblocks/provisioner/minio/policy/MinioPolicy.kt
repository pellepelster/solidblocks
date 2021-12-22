package de.solidblocks.provisioner.minio.policy

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.minio.MinioMcWrapper

class MinioPolicy(val name: String, val policy: MinioMcWrapper.Policy) :
    IMinioPolicyLookup,
    IInfrastructureResource<MinioPolicy, MinioPolicyRuntime> {

    override fun name() = name

    override fun id() = name
}
