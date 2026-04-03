package de.solidblocks.systemd

import de.solidblocks.shell.FilePermissions
import de.solidblocks.shell.ShellScript
import de.solidblocks.shell.SystemDLibrary
import de.solidblocks.shell.WriteFile
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

interface SystemDConfig {
  val extension: String
  val name: String

  fun fullUnitName() = "$name.$extension"

  fun render(): String
}

class SystemDTimer(
    override val name: String,
    val unit: Unit,
    val timer: Timer,
    val install: Install,
) : SystemDConfig {
  override fun render(): String {
    val sw = StringWriter()
    sw.appendLine(unit.render())
    sw.appendLine(timer.render())
    sw.append(install.render())
    return sw.toString()
  }

  override val extension = "timer"
}

class SystemDService(
    override val name: String,
    val unit: Unit,
    val service: Service,
    val install: Install? = null,
) : SystemDConfig {
  override fun render(): String {
    val sw = StringWriter()

    sw.appendLine(unit.render())

    sw.appendLine("[Service]")
    if (service.type != null) {
      sw.appendLine("Type=${service.type}")
    }

    service.environment.entries.forEach { sw.appendLine("Environment=\"${it.key}=${it.value}\"") }
    service.environmentFiles.forEach { sw.appendLine("EnvironmentFile=$it") }
    sw.appendLine("ExecStart=${service.execStart.joinToString(" ")}")
    service.stateDirectory?.let { sw.appendLine("StateDirectory=$it") }
    service.workingDirectory?.let { sw.appendLine("WorkingDirectory=$it") }
    service.limitNOFILE?.let { sw.appendLine("LimitNOFILE=$it") }
    sw.appendLine()

    if (install != null) {
      sw.append(install.render())
    }

    return sw.toString()
  }

  override val extension = "service"
}

class Unit(
    val description: String,
    val after: List<Target>? = listOf(Target.NETWORK_ONLINE_TARGET),
    val requires: List<Target>? = listOf(Target.NETWORK_ONLINE_TARGET),
    val wants: List<Target>? = null,
) {
  fun render(): String {
    val sw = StringWriter()

    sw.appendLine("[Unit]")
    sw.appendLine("Description=$description")
    if (after != null) {
      sw.appendLine("After=${after.joinToString(" ")}")
    }
    if (requires != null) {
      sw.appendLine("Requires=${requires.joinToString(" ")}")
    }
    if (wants != null) {
      sw.appendLine("Wants=${wants.joinToString(" ")}")
    }

    return sw.toString()
  }
}

sealed interface OnCalendar {
  fun expression(): String
}

class Daily : OnCalendar {
  override fun expression() = "daily"
}

class Hourly : OnCalendar {
  override fun expression() = "hourly"
}

class Timer(val onCalendar: OnCalendar, val unit: String) {
  fun render(): String {
    val sw = StringWriter()
    sw.appendLine("[Timer]")
    sw.appendLine("OnCalendar=${onCalendar.expression()}")
    sw.appendLine("Unit=$unit")
    return sw.toString()
  }
}

enum class ServiceType {
  simple,
}

class Service(
    val execStart: List<String>,
    val restart: Restart = Restart.ALWAYS,
    val environment: Map<String, String> = emptyMap(),
    val environmentFiles: List<String> = emptyList(),
    val workingDirectory: String? = null,
    val stateDirectory: String? = null,
    val limitNOFILE: Int? = null,
    val execDown: List<String>? = null,
    val type: ServiceType? = null,
)

class Install(val wantedBy: Target = Target.MULTI_USER_TARGET) {
  fun render(): String {
    val sw = StringWriter()
    sw.appendLine("[Install]")
    sw.appendLine("WantedBy=$wantedBy")

    return sw.toString()
  }
}

fun ShellScript.installSystemDUnit(config: SystemDConfig) {
  addCommand(
      WriteFile(
          config.render().toByteArray(),
          "/etc/systemd/system/${config.fullUnitName()}",
          FilePermissions.RW_R__R__,
      ),
  )
  addCommand(SystemDLibrary.DaemonReload())
  addCommand(SystemDLibrary.Enable(config.fullUnitName()))
}
