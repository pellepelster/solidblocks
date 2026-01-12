package de.solidblocks.systemd

import java.io.StringWriter


enum class Target(var target: String) {
    MULTI_USER("multi-user.target"),
    NETWORK_ONLINE("network-online.target");

    override fun toString(): String {
        return target
    }
}

class SystemdConfig(
    val unit: Unit,
    val service: Service,
    val install: Install = Install()
) {
    fun render(): String {
        val sw = StringWriter()

        sw.appendLine("[Unit]")
        sw.appendLine("Description=${unit.description}")
        sw.appendLine("After=${unit.after}")
        sw.appendLine("Wants=${unit.wants}")
        sw.appendLine()

        sw.appendLine("[Service]")
        service.environment.entries.forEach {
            sw.appendLine("Environment=\"${it.key}=${it.value}\"")
        }
        sw.appendLine("ExecStart=${service.execStart.joinToString(" ")}")
        service.stateDirectory?.let {
            sw.appendLine("StateDirectory=${it}")
        }
        service.limitNOFILE?.let {
            sw.appendLine("LimitNOFILE=${it}")
        }
        sw.appendLine()

        sw.appendLine("[Install]")
        sw.appendLine("WantedBy=${install.wantedBy}")

        return sw.toString()
    }
}

class Unit(
    val description: String,
    val after: Target = Target.NETWORK_ONLINE,
    val wants: Target = Target.NETWORK_ONLINE,
)

class Service(
    val execStart: List<String>,
    val environment: Map<String, String> = emptyMap(),
    val stateDirectory: String? = null,
    val limitNOFILE: Int? = null
)

class Install(
    val wantedBy: Target = Target.MULTI_USER
)