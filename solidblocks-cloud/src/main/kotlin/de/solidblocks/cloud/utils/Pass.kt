package de.solidblocks.cloud.utils

import kotlin.io.path.Path

val DEFAULT_PASS_DIR = "${Path(System.getProperty("user.home")).toAbsolutePath()}/.password-store"

fun passInsert(path: String, secret: String, passwordStoreDir: String) = runCommand(
    listOf("pass", "insert", "--multiline", "--force", path),
    secret,
    env = mapOf("PASSWORD_STORE_DIR" to passwordStoreDir),
)

fun passShow(path: String, passwordStoreDir: String) = runCommand(
    listOf("pass", path),
    env = mapOf("PASSWORD_STORE_DIR" to passwordStoreDir),
)
