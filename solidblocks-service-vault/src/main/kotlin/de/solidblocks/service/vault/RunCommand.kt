package de.solidblocks.service.vault

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.base.ServiceReference
import de.solidblocks.vault.VaultManager
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    override fun run() {

        val reference = ServiceReference("xxx", "yyy", "service1")
        val service = VaultServiceManager(reference, "/storage/local", VaultManager("xx", "xx", reference.environmentReference))
        service.start()
    }
}
