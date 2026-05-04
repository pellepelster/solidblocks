package de.solidblocks.cloud.status

data class NeedRestart(val expectedKernel: String, val currentKernel: String)

fun String.parseNeedRestart(): NeedRestart {
    val currentKernel = this.lines().single { it.startsWith("NEEDRESTART-KCUR") }.split(":").last().trim()
    val expectedKernel = this.lines().single { it.startsWith("NEEDRESTART-KEXP") }.split(":").last().trim()
    return NeedRestart(expectedKernel, currentKernel)
}
