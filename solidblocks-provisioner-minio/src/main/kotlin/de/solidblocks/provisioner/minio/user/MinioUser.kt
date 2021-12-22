package de.solidblocks.provisioner.minio.user

import de.solidblocks.core.IInfrastructureResource

class MinioUser(val name: String, val secretKey: String) :
    IMinioUserLookup,
    IInfrastructureResource<MinioUser, MinioUserRuntime> {

    override fun name() = name

    override fun id() = name
}
