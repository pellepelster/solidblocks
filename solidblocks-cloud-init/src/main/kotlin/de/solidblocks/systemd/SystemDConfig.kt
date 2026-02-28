package de.solidblocks.systemd

import java.io.StringWriter

enum class Restart(var target: String) {
  NO("no"),
  ON_SUCCESS("on-success"),
  ON_FAILURE("on-failure"),
  ON_ABNORMAL("on-abnormal"),
  ON_WATCHDOG("on-watchdog"),
  ON_ABORT("on-abort"),
  ALWAYS("always"),
}

enum class Target(var target: String) {
  MULTI_USER_TARGET("multi-user.target"),
  NETWORK_ONLINE_TARGET("network-online.target"),
  DOCKER_SERVICE("docker.service"),
  ;

  override fun toString(): String = target
}

class SystemdConfig(val unit: Unit, val service: Service, val install: Install = Install()) {
  fun render(): String {
    val sw = StringWriter()

    sw.appendLine("[Unit]")
    sw.appendLine("Description=${unit.description}")
    if (unit.after != null) {
      sw.appendLine("After=${unit.after.joinToString(" ")}")
    }
    if (unit.requires != null) {
      sw.appendLine("Requires=${unit.requires.joinToString(" ")}")
    }
    if (unit.wants != null) {
      sw.appendLine("Wants=${unit.wants.joinToString(" ")}")
    }
    sw.appendLine()

    sw.appendLine("[Service]")
    service.environment.entries.forEach { sw.appendLine("Environment=\"${it.key}=${it.value}\"") }
    sw.appendLine("ExecStart=${service.execStart.joinToString(" ")}")
    service.stateDirectory?.let { sw.appendLine("StateDirectory=$it") }
    service.workingDirectory?.let { sw.appendLine("WorkingDirectory=$it") }
    service.limitNOFILE?.let { sw.appendLine("LimitNOFILE=$it") }
    sw.appendLine()

    sw.appendLine("[Install]")
    sw.appendLine("WantedBy=${install.wantedBy}")

    return sw.toString()
  }
}

class Unit(
    val description: String,
    val after: List<Target>? = listOf(Target.NETWORK_ONLINE_TARGET),
    val requires: List<Target>? = listOf(Target.NETWORK_ONLINE_TARGET),
    val wants: List<Target>? = null,
)

class Service(
    val execStart: List<String>,
    val restart: Restart = Restart.ALWAYS,
    val environment: Map<String, String> = emptyMap(),
    val workingDirectory: String? = null,
    val stateDirectory: String? = null,
    val limitNOFILE: Int? = null,
    val execDown: List<String>? = null,
)

class Install(val wantedBy: Target = Target.MULTI_USER_TARGET)
