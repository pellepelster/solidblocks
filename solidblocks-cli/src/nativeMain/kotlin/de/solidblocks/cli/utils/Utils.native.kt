package de.solidblocks.cli.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
public actual fun getenv(name: String): String? = getenv(name)?.toKString()
