package de.solidblocks.cloud.api.health

interface HealthCheck {
  fun check(address: String, port: Int): HealthStatus
}
