package de.solidblocks.shell

interface LibraryCommand {
  fun toShell(): List<String>
}
