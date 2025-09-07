package de.solidblocks.cli.docs.ansible

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path

class AnsibleCommand : CliktCommand(name = "ansible") {

  override fun help(context: Context) = "generate hugo docs from ansible collections"

  private val collection by
      option(
              "--collection",
              help = "directory containing the Ansible collection",
          )
          .path(mustExist = true)
          .required()

  private val target by
      option(
              "--target",
              help = "target directory for generated Hugo markdown",
          )
          .path(mustExist = true)
          .required()

  override fun run() {
    AnsibleCollectionHugoGenerator(collection, target).run()
  }
}
