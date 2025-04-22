package de.solidblocks.cli.utils

public actual fun getenv(name: String): String? = System.getenv(name)