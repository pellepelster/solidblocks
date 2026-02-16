package de.solidblocks.cli.github

import de.solidblocks.utils.logError
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logWarning
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class GitHubContainerCleaner(
    private val username: String,
    private val containerRegistry: String,
    private val githubToken: String,
    private val doDelete: Boolean,
    private val deleteUntagged: Boolean,
    private val tagFilters: List<String>,
) {
  private val baseUrl = "https://api.github.com"

  private fun createClient() =
      HttpClient(Java) {
        install(ContentNegotiation) {
          json(
              Json {
                ignoreUnknownKeys = true
                prettyPrint = true
              },
          )
        }
      }

  private fun parseNextLink(linkHeader: String): String? {
    val links = linkHeader.split(",")

    for (link in links) {
      val parts = link.trim().split(";")
      if (parts.size == 2) {
        val url = parts[0].trim().removeSurrounding("<", ">")
        val rel = parts[1].trim()
        if (rel.contains("rel=\"next\"")) {
          return url
        }
      }
    }

    return null
  }

  private suspend fun HttpClient.fetchContainerVersions(block: (List<PackageVersion>) -> Unit) {
    var nextUrl: String? =
        "$baseUrl/users/$username/packages/container/$containerRegistry/versions?per_page=100"

    while (nextUrl != null) {
      val response =
          this.get(nextUrl) {
            headers {
              append(HttpHeaders.Accept, "application/vnd.github+json")
              append(HttpHeaders.Authorization, "Bearer $githubToken")
              append("X-GitHub-Api-Version", "2022-11-28")
            }
          }

      if (!response.status.isSuccess()) {
        throw RuntimeException("failed to fetch package versions: ${response.status}")
      }

      val versions: List<PackageVersion> = response.body()
      block(versions)
      val linkHeader =
          response.headers[HttpHeaders.Link]
              ?: throw RuntimeException("failed to fetch link header")
      nextUrl = parseNextLink(linkHeader)
    }
  }

  private suspend fun HttpClient.deleteContainerVersion(version: Long) =
      try {
        val response =
            this.delete(
                "$baseUrl/users/$username/packages/container/$containerRegistry/versions/$version",
            ) {
              headers {
                append(HttpHeaders.Accept, "application/vnd.github+json")
                append(HttpHeaders.Authorization, "Bearer $githubToken")
                append("X-GitHub-Api-Version", "2022-11-28")
              }
            }

        if (response.status == HttpStatusCode.NoContent || response.status.isSuccess()) {
          true
        } else {
          logError("failed to delete version '$version' (HTTP ${response.status})")
          false
        }
      } catch (e: Exception) {
        logError("failed to delete version $version: ${e.message}")
        false
      }

  fun cleanupContainers() {
    runBlocking {
      createClient().use { client ->
        logInfo("fetching all package versions for $username/$containerRegistry")
        val allContainers = mutableListOf<PackageVersion>()

        client.fetchContainerVersions {
          logInfo("fetched '${it.size}' versions")
          allContainers.addAll(it)
        }

        val tagFilterRegex = tagFilters.map { it.toRegex() }

        for (container in allContainers) {
          if (container.metadata?.container?.tags == null) {
            logWarning("not metadata found for '${container.name}'")
            continue
          }

          if (container.metadata.container.tags.isEmpty()) {
            if (deleteUntagged) {
              if (doDelete) {
                logWarning("deleting untagged image '${container.name}'")
                runBlocking { client.deleteContainerVersion(container.id) }
              } else {
                logWarning("would delete untagged image '${container.name}'")
              }
            } else {
              logInfo("keeping untagged image '${container.name}'")
            }
          } else {
            if (
                container.metadata.container.tags.all { tag ->
                  tagFilterRegex.any { it.matches(tag) }
                }
            ) {
              if (doDelete) {
                logWarning(
                    "deleting tagged image '${container.name}' (${
                                        container.metadata.container.tags.joinToString(
                                            ", ",
                                        )
                                    })",
                )
                runBlocking { client.deleteContainerVersion(container.id) }
              } else {
                logWarning(
                    "would delete tagged image '${container.name}'  (${
                                        container.metadata.container.tags.joinToString(
                                            ", ",
                                        )
                                    })",
                )
              }
            } else {
              logInfo(
                  "keeping tagged image '${container.name}' (${
                                    container.metadata.container.tags.joinToString(
                                        ", ",
                                    )
                                })",
              )
            }
          }
        }
      }
    }
  }
}
