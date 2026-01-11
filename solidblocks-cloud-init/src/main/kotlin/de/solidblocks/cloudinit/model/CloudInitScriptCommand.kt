package de.solidblocks.cloudinit.model

interface CloudInitScriptCommand {
    fun toShell(): List<String>
}
