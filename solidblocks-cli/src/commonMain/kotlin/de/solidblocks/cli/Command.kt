package de.solidblocks.cli

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio

fun commandExists(command: String) = Command("which")
    .args(listOf(command)).stdout(Stdio.Null).spawn().wait() == 0
