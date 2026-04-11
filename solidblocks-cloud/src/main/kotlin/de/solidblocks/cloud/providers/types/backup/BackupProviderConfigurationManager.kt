package de.solidblocks.cloud.providers.types.backup

import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentReference
import de.solidblocks.cloud.providers.ProviderConfigurationManager
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.backup.aws.S3BackupProviderConfigurationRuntime
import de.solidblocks.cloud.providers.backup.aws.S3BackupProviderManager
import de.solidblocks.cloud.providers.backup.local.LocalBackupProviderConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.aws.iam.AwsIamUser
import de.solidblocks.cloud.provisioner.aws.s3.AwsS3Bucket
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloudinit.BackupConfiguration
import de.solidblocks.cloudinit.LocalBackupTarget
import de.solidblocks.cloudinit.S3BackupTarget

interface BackupProviderConfigurationManager<
    C : BackupProviderConfiguration,
    R : ProviderConfigurationRuntime,
    > : ProviderConfigurationManager<C, R>

fun backupSecretResource(runtime: CloudConfigurationRuntime) = PassSecret(
    secretPath(runtime, listOf("backup", "password")),
    length = 32,
    allowedChars = ('a'..'f') + ('0'..'9'),
)

fun createBackupResources(
    runtime: BackupProviderConfigurationRuntime,
    cloud: CloudConfigurationRuntime,
    serverName: String,
    service: ServiceConfigurationRuntime,
    environment: EnvironmentReference,
): Pair<Set<BaseInfrastructureResource<out BaseInfrastructureResourceRuntime>>, HetznerVolume?> {
    val bucketName = S3BackupProviderManager.bucketName(environment, service)
    val iamUserName = S3BackupProviderManager.iamUserName(environment, service)

    return when (runtime) {
        is S3BackupProviderConfigurationRuntime -> {
            val bucketArn = "arn:aws:s3:::$bucketName"
            setOf(
                AwsS3Bucket(bucketName, runtime.region),
                AwsIamUser(iamUserName, S3BackupProviderManager.backupBucketPolicy(bucketArn)),
            ) to null
        }

        else -> {
            val backupVolume = HetznerVolume(
                serverName + "-backup",
                service.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
                service.backup.backupVolumeSizeWithDefault(service.instance.volumeSize),
                emptyMap(),
            )
            setOf(backupVolume) to backupVolume
        }
    }
}

fun createBackupConfiguration(
    runtime: BackupProviderConfigurationRuntime,
    cloud: CloudConfigurationRuntime,
    service: ServiceConfigurationRuntime,
    context: CloudProvisionerContext,
    backupVolume: HetznerVolume?,
): BackupConfiguration {
    val backupPassword = backupSecretResource(cloud).asLookup()
    val bucketName = S3BackupProviderManager.bucketName(context.environment, service)
    val iamUserName = S3BackupProviderManager.iamUserName(context.environment, service)

    return when (runtime) {
        is S3BackupProviderConfigurationRuntime -> {
            BackupConfiguration(
                context.ensureLookup(backupPassword).secret,
                S3BackupTarget(
                    bucketName,
                    context.ensureLookup(PassSecretLookup(S3BackupProviderManager.accessKeySecretPath(context.environment, iamUserName))).secret,
                    context.ensureLookup(PassSecretLookup(S3BackupProviderManager.secretKeySecretPath(context.environment, iamUserName))).secret,
                ),
            )
        }

        is LocalBackupProviderConfigurationRuntime -> {
            BackupConfiguration(context.ensureLookup(backupPassword).secret, LocalBackupTarget(context.ensureLookup(backupVolume!!.asLookup()).device))
        }

        else -> throw RuntimeException("unknown backup provider '${runtime.javaClass.name}'")
    }
}
