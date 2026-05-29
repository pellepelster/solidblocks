package de.solidblocks.cloud

import de.solidblocks.cloud.github.GitHubApi
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

class GitHubApiTest {
    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
    fun testFlow() {
        val api = GitHubApi(System.getenv("GITHUB_TOKEN"))
        runBlocking {
            val token = api.runners.createRepoRegistrationToken("pellepelster", "solidblocks")
            token.token shouldStartWith "AA"
        }
    }
}
