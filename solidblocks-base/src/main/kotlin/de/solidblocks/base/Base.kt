package de.solidblocks.base

import com.jcabi.manifests.Manifests

public fun solidblocksVersion() = try {
    Manifests.read("Solidblocks-Version")
} catch (e: Exception) {
    "snapshot"
}
