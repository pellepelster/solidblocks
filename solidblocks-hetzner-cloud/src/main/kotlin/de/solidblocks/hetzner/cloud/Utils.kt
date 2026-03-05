package de.solidblocks.hetzner.cloud

public fun String.pascalCaseToWhiteSpace() = this.replace(Regex("([A-Z])"), " $1").trim()
