package de.solidblocks.base

import com.jcabi.manifests.Manifests

fun solidblocksVersion(): String = try {
    Manifests.read("Solidblocks-Version")
} catch (e: Exception) {
    "snapshot"
}

fun validateId(id: String) = "[a-z]+[a-z0-9-]{1,61}[a-z0-9]+".toRegex().matches(id)
