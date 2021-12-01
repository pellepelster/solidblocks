package de.solidblocks.provisioner.consul.policy

public class ConsulRuleBuilder {

    private val policies: MutableList<Policy> = mutableListOf()

    fun addKeyPrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("key_prefix", path, privilege))
        return this
    }

    fun asPolicy(): String {
        return policies.map {
            "${it.type} \"${it.path}\" { policy = \"${it.privilege}\" } \n"
        }.joinToString(separator = "\n")
    }

    fun addNodePrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("node_prefix", path, privilege))
        return this
    }

    fun addEventPrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("event_prefix", path, privilege))
        return this
    }

    fun addEvent(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("event", path, privilege))
        return this
    }

    fun addAgentPrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("agent_prefix", path, privilege))
        return this
    }

    fun addAgent(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("agent", path, privilege))
        return this
    }

    fun addSessionPrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("session_prefix", path, privilege))
        return this
    }

    fun addServicePrefix(path: String, privilege: Privileges): ConsulRuleBuilder {
        policies.add(Policy("service_prefix", path, privilege))
        return this
    }
}