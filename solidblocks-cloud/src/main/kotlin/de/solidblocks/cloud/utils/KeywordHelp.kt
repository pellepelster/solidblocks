package de.solidblocks.cloud.utils

data class KeywordHelp(val description: String)

enum class KeywordType {
  STRING,
  ENUM,
}

data class Keyword(val name: String, val type: KeywordType, val help: KeywordHelp)
