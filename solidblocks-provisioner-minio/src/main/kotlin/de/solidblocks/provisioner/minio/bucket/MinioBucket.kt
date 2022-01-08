package de.solidblocks.provisioner.minio.bucket

import de.solidblocks.core.IInfrastructureResource

class MinioBucket(override val name: String) :
    IMinioBucketLookup,
    IInfrastructureResource<MinioBucket, MinioBucketRuntime>
