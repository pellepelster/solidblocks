package de.solidblocks.cloudinit

import de.solidblocks.cloudinit.model.CloudInit
import de.solidblocks.cloudinit.model.Mount
import org.junit.jupiter.api.Test

class CloudInitTest {

    @Test
    fun test() {
        val cloudInit = CloudInit()
        cloudInit.mounts.add(Mount("dd", "dd"))

    }
}