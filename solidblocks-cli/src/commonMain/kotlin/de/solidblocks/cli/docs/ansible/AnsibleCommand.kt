package de.solidblocks.cli.docs.ansible

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import okio.Path.Companion.toPath

class AnsibleCommand : CliktCommand(name = "ansible") {

    override fun help(context: Context) = "generate hugo docs from ansible collections"

    private val collection by option(
        "--collection",
        help = "directory containing the Ansible collection",
    ).required()

    private val target by option(
        "--target",
        help = "target directory for generated Hugo markdown",
    ).required()


    override fun run() {
        AnsibleCollectionHugoGenerator(collection.toPath(), target.toPath()).run()
    }
}