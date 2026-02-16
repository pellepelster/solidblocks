package de.solidblocks.cli.github

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class GithubRegistryCleanCommand : CliktCommand(name = "registry-clean") {

  override fun help(context: Context) = "Cleanup images from a GitHub container registry"

  private val githubToken by
      option(
              "--gh-token",
              help = "the GitHub token",
              envvar = "GH_TOKEN",
          )
          .required()

  private val doDelete by
      option(
              "--do-delete",
              help = "actually delete images, if deletions are only logged",
          )
          .flag(default = false)

  val username by
      option("--username").help { "GitHub username owning the container registry" }.required()

  val containerRegistry by
      option("--container-registry").help { "Name of the container registry" }.required()

  val tagFilter: List<String> by
      option("--tag-filter")
          .help {
            "containers where all tags match the filter regex will be deleted. Multiple filter can be supplied to match containers with multiple tags."
          }
          .multiple()

  private val deleteUntagged by
      option(
              "--delete-untagged",
              help = "delete containers that do not have any tag set",
          )
          .flag(default = false)

  override fun run() {
    runBlocking {
      GitHubContainerCleaner(
              username,
              containerRegistry,
              githubToken,
              doDelete,
              deleteUntagged,
              tagFilter,
          )
          .cleanupContainers()
    }
  }
}
