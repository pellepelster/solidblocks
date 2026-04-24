package de.solidblocks.cloud

import de.solidblocks.cloud.github.GitHubApi
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GitHubApiTest {
    @Test
    fun testFlow() {
        val api = GitHubApi(System.getenv("GITHUB_TOKEN"))
        runBlocking {
            val token = api.runners.createRepoRegistrationToken("pellepelster", "solidblocks")
            token.token shouldStartWith "AA"
        }
    }
}
