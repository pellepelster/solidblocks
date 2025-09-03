package de.solidblocks.cli.utils


data class Help(val description: String)

data class KeywordHelp(
    val example: String,
    val description: String,
)

enum class KeywordType { string, enum }

data class Keyword(
    val name: String,
    val type: KeywordType,
    val help: KeywordHelp
)
