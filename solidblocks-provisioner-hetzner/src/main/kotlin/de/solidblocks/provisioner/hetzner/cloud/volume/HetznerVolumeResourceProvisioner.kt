package de.solidblocks.provisioner.hetzner.cloud.volume

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ChangeProtectionRequest
import me.tomsdevsn.hetznercloud.objects.request.UpdateVolumeRequest
import me.tomsdevsn.hetznercloud.objects.request.VolumeRequest
import mu.KotlinLogging

class HetznerVolumeResourceProvisioner(hetznerCloudAPI: HetznerCloudAPI) :
    IResourceLookupProvider<IVolumeLookup, VolumeRuntime>,
    IInfrastructureResourceProvisioner<Volume,
        VolumeRuntime>,
    BaseHetznerProvisioner<Volume, VolumeRuntime, HetznerCloudAPI>(hetznerCloudAPI) {

    private val logger = KotlinLogging.logger {}

    override fun destroyAll(): Boolean {
        logger.info { "destroying all volumes" }

        return checkedApiCall {
            it.volumes.volumes
        }.mapSuccessNonNullBoolean {

            it.filterNot { it.protection.delete }.map { volume ->
                ensureDetached(volume.id).mapResult {
                    logger.info { "destroying volume '${volume.name}'" }
                    destroy(volume.id)
                }.mapSuccessBoolean()
            }.ifEmpty { listOf(true) }.all { it }.also {
                if (!it) {
                    logger.error { "destroying all volumes failed" }
                }
            }
        }
    }

    private fun destroy(id: Long) = checkedApiCall {
        it.deleteVolume(id)
    }.mapSuccessBoolean()

    private fun ensureDetached(id: Long): Result<*> {
        return checkedApiCall {
            it.getVolume(id)
        }.mapNonNullResult {

            if (it.volume.server == null) {
                return@mapNonNullResult it
            }

            checkedApiCall {
                it.detachVolume(id).action
            }.mapNonNullResult {
                waitForActions(listOf(it)) { api, action ->
                    val actionResult = api.getActionOfVolume(id, action.id).action
                    logger.info { "waiting for action '${action.command}' to finish, current status is '${action.status}'" }
                    actionResult.finished != null
                }
            }
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
        request.name(resource.id)
        request.size(16L)
        request.location(resource.location)
        request.format("ext4")
        request.labels(resource.labels)

        return checkedApiCall {
            logger.info { "creating volume '${resource.id}'" }
            val v = it.createVolume(request.build())
            val p = it.changeVolumeProtection(v.volume.id, ChangeProtectionRequest.builder().delete(true).build())

            v.volume.id to ArrayList(v.nextActions).let {
                it.add(p.action)
                it
            }
        }.mapNonNullResult { r ->
            logger.info {
                "volume '${resource.id}' created, waiting for actions '${r.second.map { it.command }.joinToString(", ")}' to finish"
            }
            waitForActions(
                r.second
            ) { api, action ->
                val actionResult = api.getActionOfVolume(r.first, action.id).action
                logger.info { "waiting for action '${action.command}' to finish for volume '${resource.id}', current status is '${action.status}'" }
                actionResult.finished != null
            }
        }
    }

    override fun apply(resource: Volume): Result<*> {
        return lookup(resource).mapResult { volume ->

            if (volume == null) {
                return@mapResult createVolume(resource)
            }

            val request = UpdateVolumeRequest.builder()
            request.name(resource.id)
            checkedApiCall {
                logger.info { "updating volume '${resource.id}'" }
                it.updateVolume(volume.id.toLong(), request.build())
            }
        }
    }

    override fun destroy(resource: Volume): Boolean {
        return lookup(resource).mapNonNullResult {
            destroy(it.id.toLong())
        }.mapSuccessNonNullBoolean { true }
    }

    override fun getResourceType(): Class<*> {
        return Volume::class.java
    }

    override fun lookup(lookup: IVolumeLookup): Result<VolumeRuntime> {
        return checkedApiCall {
            it.volumes.volumes.firstOrNull { it.name == lookup.id() }
        }.mapNonNullResult {
            VolumeRuntime(it.id.toString(), it.linuxDevice, it.server)
        }
    }

    override fun getLookupType(): Class<*> {
        return IVolumeLookup::class.java
    }
}
