package de.solidblocks.cloud

import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.provisioner.minio.bucket.MinioBucket

object MinioCloudConfiguration {

    fun createServiceBackupConfig(
        parentResourceGroups: Set<ResourceGroup>,
        reference: ServiceReference
    ): ResourceGroup {
        val group = ResourceGroup("${serviceId(reference)}-backup", parentResourceGroups)

        val bucket = MinioBucket(serviceId(reference))
        group.addResource(bucket)

        return group
    }
}
