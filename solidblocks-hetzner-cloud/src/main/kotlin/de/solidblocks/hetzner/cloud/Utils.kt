package de.solidblocks.hetzner.cloud

fun String.pascalCaseToWhiteSpace() = this.replace(Regex("([A-Z])"), " $1").trim()
