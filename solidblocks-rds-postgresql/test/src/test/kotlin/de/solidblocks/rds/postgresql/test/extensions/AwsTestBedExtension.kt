package de.solidblocks.rds.postgresql.test.extensions

import org.junit.jupiter.api.extension.*


class AwsTestBedExtension : ParameterResolver, AfterEachCallback, AfterAllCallback {

    private val testBeds = mutableMapOf<String, AwsTestBed>()

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == AwsTestBed::class.java
    }

    @Throws(ParameterResolutionException::class)
    override fun resolveParameter(
        parameterContext: ParameterContext,
        context: ExtensionContext
    ): Any {
        logger.info { "[test] creating testbed" }

        return testBeds.getOrPut(context.uniqueId) {
            AwsTestBed()
        }.also { it.initTestbed() }
    }

    override fun afterEach(context: ExtensionContext) {
        logger.info { "[test] cleaning testbed '${context.uniqueId}'" }
        testBeds[context.uniqueId]?.destroyTestBed()
    }

    override fun afterAll(p0: ExtensionContext?) {
        logger.info { "[test] cleaning all testbeds" }
        testBeds.values.forEach { it.destroyTestBed() }
    }

}