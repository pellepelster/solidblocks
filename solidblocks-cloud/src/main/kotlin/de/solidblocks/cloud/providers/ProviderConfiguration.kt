package de.solidblocks.cloud.providers

val DEFAULT_NAME = "default"

interface ProviderConfiguration {
  val type: String
  val name: String
}
