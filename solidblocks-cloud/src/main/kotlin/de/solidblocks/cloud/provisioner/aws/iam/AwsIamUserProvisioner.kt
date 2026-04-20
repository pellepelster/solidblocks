package de.solidblocks.cloud.provisioner.aws.iam

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.iam.IamClient
import aws.sdk.kotlin.services.iam.model.CreateAccessKeyRequest
import aws.sdk.kotlin.services.iam.model.CreateUserRequest
import aws.sdk.kotlin.services.iam.model.DeleteAccessKeyRequest
import aws.sdk.kotlin.services.iam.model.DeleteUserPolicyRequest
import aws.sdk.kotlin.services.iam.model.DeleteUserRequest
import aws.sdk.kotlin.services.iam.model.GetUserPolicyRequest
import aws.sdk.kotlin.services.iam.model.GetUserRequest
import aws.sdk.kotlin.services.iam.model.ListAccessKeysRequest
import aws.sdk.kotlin.services.iam.model.ListUserPoliciesRequest
import aws.sdk.kotlin.services.iam.model.NoSuchEntityException
import aws.sdk.kotlin.services.iam.model.PutUserPolicyRequest
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.providers.backup.aws.S3BackupProviderManager
import de.solidblocks.cloud.providers.backup.aws.S3BackupProviderManager.Companion.accessKeySecretPath
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import kotlin.reflect.KClass

class AwsIamUserProvisioner(
    private val accessKeyId: String,
    private val secretAccessKey: String,
) : ResourceLookupProvider<AwsIamUserLookup, AwsIamUserRuntime>,
    InfrastructureResourceProvisioner<AwsIamUser, AwsIamUserRuntime> {

    private val json = Json { prettyPrint = false }

    override suspend fun lookup(lookup: AwsIamUserLookup, context: ProvisionerContext): AwsIamUserRuntime? = iamClient().use { client ->
        val user = try {
            client.getUser(GetUserRequest { userName = lookup.name }).user
        } catch (e: NoSuchEntityException) {
            null
        }

        if (user == null) {
            return@use null
        }

        val inlinePolicy = try {
            val encoded = client.getUserPolicy(
                GetUserPolicyRequest {
                    userName = lookup.name
                    policyName = "${lookup.name}-policy"
                },
            ).policyDocument
            URLDecoder.decode(encoded, Charsets.UTF_8)
        } catch (e: NoSuchEntityException) {
            ""
        }

        AwsIamUserRuntime(
            name = user.userName,
            arn = user.arn,
            inlinePolicy = inlinePolicy,
        )
    }

    override suspend fun diff(resource: AwsIamUser, context: ProvisionerDiffContext): ResourceDiff {
        val runtime = lookup(resource.asLookup(), context) ?: return ResourceDiff(resource, missing)

        val changes = mutableListOf<ResourceDiffItem>()

        if (!policiesEqual(resource.inlinePolicy, runtime.inlinePolicy)) {
            changes.add(
                ResourceDiffItem(
                    "policy",
                    changed = true,
                    expectedValue = normalizePolicy(resource.inlinePolicy),
                    actualValue = normalizePolicy(runtime.inlinePolicy),
                ),
            )
        }

        return if (changes.isEmpty()) {
            ResourceDiff(resource, up_to_date)
        } else {
            ResourceDiff(resource, has_changes, changes = changes)
        }
    }

    override suspend fun apply(resource: AwsIamUser, context: ProvisionerApplyContext, log: LogContext): Result<AwsIamUserRuntime> = iamClient().use { client ->
        val exists = try {
            client.getUser(GetUserRequest { userName = resource.name })
            true
        } catch (e: NoSuchEntityException) {
            false
        }

        if (!exists) {
            log.info("creating IAM user '${resource.name}'")
            client.createUser(CreateUserRequest { userName = resource.name })

            val keys = client.createAccessKey(
                CreateAccessKeyRequest {
                    userName = resource.name
                },
            )

            when (val result = context.createSecret(accessKeySecretPath(context.environment, resource.name), keys.accessKey!!.accessKeyId)) {
                is Error<Unit> -> return@use Error(result.error)
                is Success<Unit> -> {
                }
            }

            when (val result = context.createSecret(S3BackupProviderManager.secretKeySecretPath(context.environment, resource.name), keys.accessKey!!.secretAccessKey)) {
                is Error<Unit> -> return@use Error(result.error)
                is Success<Unit> -> {
                }
            }
        }

        log.info("updating inline policy '${resource.policyName}' for IAM user '${resource.name}'")
        client.putUserPolicy(
            PutUserPolicyRequest {
                userName = resource.name
                policyName = resource.policyName
                policyDocument = resource.inlinePolicy
            },
        )

        lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error("error applying ${resource.logText()}")
    }

    override suspend fun destroy(resource: AwsIamUser, context: ProvisionerContext, log: LogContext): Boolean {
        iamClient().use { client ->
            try {
                val policies = client.listUserPolicies(ListUserPoliciesRequest { userName = resource.name }).policyNames
                policies.forEach { policyName ->
                    client.deleteUserPolicy(
                        DeleteUserPolicyRequest {
                            userName = resource.name
                            this.policyName = policyName
                        },
                    )
                }

                val keys = client.listAccessKeys(ListAccessKeysRequest { userName = resource.name }).accessKeyMetadata
                keys.forEach { key ->
                    client.deleteAccessKey(
                        DeleteAccessKeyRequest {
                            userName = resource.name
                            accessKeyId = key.accessKeyId
                        },
                    )
                }

                client.deleteUser(DeleteUserRequest { userName = resource.name })
            } catch (e: NoSuchEntityException) {
                return false
            }
        }
        return true
    }

    private fun normalizePolicy(policy: String): String = runCatching { json.encodeToString(json.parseToJsonElement(policy)) }.getOrDefault(policy)

    private fun policiesEqual(a: String, b: String): Boolean = normalizePolicy(a) == normalizePolicy(b)

    private fun iamClient() = IamClient {
        region = "us-east-1"
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = this@AwsIamUserProvisioner.accessKeyId
            secretAccessKey = this@AwsIamUserProvisioner.secretAccessKey
        }
    }

    override val supportedLookupType: KClass<*> = AwsIamUserLookup::class

    override val supportedResourceType: KClass<*> = AwsIamUser::class
}
