package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.KeywordHelp
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext

class TestContextUtils {}

val TEST_PROVISIONER_CONTEXT =
    ProvisionerContext(
        SSHKeyUtils.loadKey(TestContextUtils::class.java.getResource("/test_ed25519.key").readText()),
        TestContextUtils::class.java.getResource("/test_ed25519.key").path,
        "testCloudName",
        "testEnvironment",
        ProvisionersRegistry(),
    )

val TEST_LOG_CONTEXT = LogContext()

val TEST_KEYWORD_HELP = KeywordHelp("TODO")