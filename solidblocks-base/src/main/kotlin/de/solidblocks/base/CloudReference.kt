package de.solidblocks.base

fun String.toCloudReference() = CloudReference(this)

open class CloudReference(val cloud: String)
