package de.solidblocks.cli

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import de.solidblocks.cli.workflow.commands.workflowHelp
import kotlin.test.Test

class HelpTest {

    @Test
    fun workflowHelp() {

        val t = Terminal()
        t.println("This text is ${TextColors.brightBlue("colorful")}!")


        println(workflowHelp)
    }

}