package de.solidblocks.provisioner.minio.user

import de.solidblocks.core.IInfrastructureResource

class MinioUser(override val name: String, val secretKey: String) :
    IMinioUserLookup,
    IInfrastructureResource<MinioUser, MinioUserRuntime>
