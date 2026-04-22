package de.solidblocks.cloud.github.resources

import de.solidblocks.cloud.github.GitHubApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RunnerRegistrationToken(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

class GitHubRunnersApi(private val api: GitHubApi) {
    suspend fun createRepoRegistrationToken(owner: String, repo: String): RunnerRegistrationToken = api.post("repos/$owner/$repo/actions/runners/registration-token")

    suspend fun createOrgRegistrationToken(org: String): RunnerRegistrationToken = api.post("orgs/$org/actions/runners/registration-token")
}
