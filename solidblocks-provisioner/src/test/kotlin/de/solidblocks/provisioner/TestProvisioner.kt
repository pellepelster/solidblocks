package de.solidblocks.provisioner

import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.ResourceGroup
import de.solidblocks.cloud.provisioner.BaseProvisioner
import de.solidblocks.provisioner.mock.TestApplyContext
import de.solidblocks.provisioner.mock.TestDestroyContext
import de.solidblocks.provisioner.mock.TestDiffContext
import de.solidblocks.provisioner.mock.TestLookupContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.mockk

val TEST_LOG_CONTEXT = mockk<LogContext>(relaxed = true)

class TestProvisioner(val registry1: TestProvisionersRegistry) : BaseProvisioner<TestDiffContext, TestApplyContext, TestDestroyContext, TestLookupContext>(registry1) {

    private val logger = KotlinLogging.logger {}

    fun diff(resourceGroups: List<ResourceGroup>, taintCallback: (BaseInfrastructureResource<*>) -> Boolean, log: LogContext): Result<Map<ResourceGroup, List<ResourceDiff>>> =
        super.diff(resourceGroups, taintCallback, log, {
            TestDiffContext()
        })

    suspend fun apply(resources: List<BaseInfrastructureResource<*>>, context: TestApplyContext) = super.apply(resources, {
        context
    })

    fun apply(
        resourceGroupDiffs: Map<ResourceGroup, List<ResourceDiff>>,
        context: TestApplyContext
    ): Result<Unit> {
        return super.apply(
            resourceGroupDiffs, {
                context
            }, {
                context
            }, {
                mockk<TestDestroyContext>()
            }, TEST_LOG_CONTEXT
        )
    }


    var waitForEndpointResult: (Endpoint) -> Result<Unit> = { Success(Unit) }

    override suspend fun waitForEndpoint(
        it: Endpoint, context: TestApplyContext, log: LogContext
    ) = waitForEndpointResult(it)

}
