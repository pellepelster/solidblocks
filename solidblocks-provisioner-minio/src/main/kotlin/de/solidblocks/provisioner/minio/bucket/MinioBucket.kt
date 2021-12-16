package de.solidblocks.provisioner.minio.bucket

import de.solidblocks.core.IInfrastructureResource

class MinioBucket(val name: String) :
    IMinioBucketLookup,
    IInfrastructureResource<MinioBucket, MinioBucketRuntime> {

    override fun name() = name

    override fun id() = name
}
