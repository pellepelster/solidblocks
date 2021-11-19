package de.solidblocks.provisioner.hetzner.cloud

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.compute.Volume
import de.solidblocks.api.resources.infrastructure.compute.VolumeRuntime
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result
import de.solidblocks.core.reduceResults
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ChangeProtectionRequest
import me.tomsdevsn.hetznercloud.objects.request.UpdateVolumeRequest
import me.tomsdevsn.hetznercloud.objects.request.VolumeRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerVolumeResourceProvisioner(credentialsProvider: HetznerCloudCredentialsProvider) :
    BaseHetznerProvisioner<Volume, VolumeRuntime, HetznerCloudAPI>(
        { HetznerCloudAPI(credentialsProvider.defaultApiToken()) },
        Volume::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Result<*> {
        logger.info { "destroying all volumes" }

        return checkedApiCall(NullResource, HetznerCloudAPI::getVolumes) {
            it.volumes.volumes
        }.mapNonNullResult {

            it.filterNot { it.protection.delete }.map { volume ->
                ensureDetached(volume.id).mapResourceResult {
                    logger.info { "destroying volume '${volume.name}'" }
                    destroy(volume.id)
                }
            }.reduceResults()
        }
    }

    private fun destroy(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::deleteVolume) {
            it.deleteVolume(id)
        }
    }

    private fun ensureDetached(id: Long): Result<*> {
        return checkedApiCall(NullResource, HetznerCloudAPI::getVolume) {
            it.getVolume(id)
        }.mapNonNullResult {

            if (it.volume.server == null) {
                return@mapNonNullResult it
            }

            checkedApiCall(NullResource, HetznerCloudAPI::detachVolume) {
                it.detachVolume(id).action
            }.mapNonNullResult {
                waitForActions(NullResource, HetznerCloudAPI::getActionOfVolume, listOf(it)) { api, action ->
                    val actionResult = api.getActionOfVolume(id, action.id).action
                    logger.info { "waiting for action '${action.command}' to finish, current status is '${action.status}'" }
                    actionResult.finished != null
                }
            }
        }
    }

    override fun lookup(resource: Volume): Result<VolumeRuntime> {
        return checkedApiCall(resource, HetznerCloudAPI::getVolumes) {
            it.volumes.volumes.firstOrNull { it.name == resource.name }
        }.mapNonNullResult {
            VolumeRuntime(it.id.toString(), it.linuxDevice, it.server)
        }
    }

    override fun diff(resource: Volume): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, false)
            },
            {
                ResourceDiff(resource, true)
            }
        )
    }

    fun createVolume(resource: Volume): Result<*> {
        val request = VolumeRequest.builder()
        request.name(resource.name)
        request.size(16L)
        request.location(resource.location)
        request.format("ext4")

        return checkedApiCall(resource, HetznerCloudAPI::createVolume) {
            logger.info { "creating volume '${resource.name}'" }
            val v = it.createVolume(request.build())
            val p = it.changeVolumeProtection(
                v.volume.id,
                ChangeProtectionRequest.builder().delete(true).build()
            )

            v.volume.id to ArrayList(v.nextActions).let {
                it.add(p.action)
                it
            }
        }.mapNonNullResult { r ->
            logger.info {
                "volume '${resource.name}' created, waiting for actions '${
                r.second.map { it.command }.joinToString(", ")
                }' to finish"
            }
            waitForActions(
                resource,
                HetznerCloudAPI::getActionOfVolume,
                r.second
            ) { api, action ->
                val actionResult = api.getActionOfVolume(r.first, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for volume '${resource.name}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun apply(resource: Volume): Result<*> {
        return lookup(resource).mapResourceResult { volume ->

            if (volume == null) {
                return@mapResourceResult createVolume(resource)
            }

            val request = UpdateVolumeRequest.builder()
            checkedApiCall(resource, HetznerCloudAPI::createVolume) {
                logger.info { "updating volume '${resource.name}'" }
                it.updateVolume(volume.id.toLong(), request.build())
            }
        }
    }

    override fun destroy(resource: Volume): Result<*> {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }
    }
}
