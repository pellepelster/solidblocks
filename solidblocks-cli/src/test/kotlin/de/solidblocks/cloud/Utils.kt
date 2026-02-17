package de.solidblocks.cloud

import de.solidblocks.cloud.healthcheck.SSHClientTest
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext

val TEST_PROVISIONER_CONTEXT =
    ProvisionerContext(
        SSHKeyUtils.loadKey(SSHClientTest::class.java.getResource("/test_ed25519.key").readText()),
        "testCloudName",
        "testEnvironment",
        ProvisionersRegistry(),
    )

val TEST_LOG_CONTEXT = LogContext()
