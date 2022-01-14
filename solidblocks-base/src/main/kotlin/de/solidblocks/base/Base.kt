package de.solidblocks.base

import com.jcabi.manifests.Manifests

fun solidblocksVersion(): String = try {
    Manifests.read("Solidblocks-Version")
} catch (e: Exception) {
    "snapshot"
}
