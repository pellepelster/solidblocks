package de.solidblocks.utils

import java.security.MessageDigest

val sha256MessageDigest = MessageDigest.getInstance("SHA-256")

fun String.sha256Hash() = this.toByteArray().sha256Hash()

fun ByteArray.sha256Hash() = sha256MessageDigest.digest(this).toHexString()
