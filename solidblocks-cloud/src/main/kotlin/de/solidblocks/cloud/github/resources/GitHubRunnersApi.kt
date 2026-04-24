package de.solidblocks.cloud.github.resources

import de.solidblocks.cloud.github.GitHubApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RunnerRegistrationToken(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class RunnerLabel(
    val id: Long? = null,
    val name: String,
    val type: String? = null,
)

@Serializable
data class Runner(
    val id: Long,
    val name: String,
    val os: String,
    val status: String,
    val busy: Boolean,
    val labels: List<RunnerLabel> = emptyList(),
)

@Serializable
data class RunnersPage(
    @SerialName("total_count") val totalCount: Int,
    val runners: List<Runner>,
)

class GitHubRunnersApi(private val api: GitHubApi) {
    suspend fun createRepoRegistrationToken(owner: String, repo: String): RunnerRegistrationToken = api.post("repos/$owner/$repo/actions/runners/registration-token")

    suspend fun createOrgRegistrationToken(org: String): RunnerRegistrationToken = api.post("orgs/$org/actions/runners/registration-token")

    suspend fun listRepoRunners(owner: String, repo: String): List<Runner> = api.get<RunnersPage>("repos/$owner/$repo/actions/runners").runners

    suspend fun getRepoRunner(owner: String, repo: String, runnerId: Long): Runner = api.get("repos/$owner/$repo/actions/runners/$runnerId")

    suspend fun deleteRepoRunner(owner: String, repo: String, runnerId: Long) = api.delete("repos/$owner/$repo/actions/runners/$runnerId")

    suspend fun listOrgRunners(org: String): List<Runner> = api.get<RunnersPage>("orgs/$org/actions/runners").runners

    suspend fun getOrgRunner(org: String, runnerId: Long): Runner = api.get("orgs/$org/actions/runners/$runnerId")

    suspend fun deleteOrgRunner(org: String, runnerId: Long) = api.delete("orgs/$org/actions/runners/$runnerId")
}
