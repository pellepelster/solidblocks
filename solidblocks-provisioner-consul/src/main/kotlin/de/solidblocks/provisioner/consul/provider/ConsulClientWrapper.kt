package de.solidblocks.provisioner.consul.provider

import com.ecwid.consul.v1.ConsulClient

class ConsulClientWrapper(val consulClient: ConsulClient, val token: String) {
    fun <T> execute(callback: (ConsulClient, String) -> T): T {
        return callback.invoke(consulClient, token)
    }
}