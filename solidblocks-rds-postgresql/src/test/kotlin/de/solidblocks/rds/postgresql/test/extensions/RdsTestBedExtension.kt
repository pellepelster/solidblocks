package de.solidblocks.rds.postgresql.test.extensions

import org.junit.jupiter.api.extension.*


class RdsTestBedExtension : ParameterResolver, AfterEachCallback, AfterAllCallback {

    private val testBeds = mutableMapOf<String, RdsTestBed>()

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == RdsTestBed::class.java
    }

    @Throws(ParameterResolutionException::class)
    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        logger.info { "[test] creating RDS testbed" }
        return testBeds.getOrPut(extensionContext.uniqueId) {
            RdsTestBed()
        }
    }

    override fun afterEach(context: ExtensionContext) {
        logger.info { "[test] cleaning RDS testbed '${context.uniqueId}'" }
        testBeds[context.uniqueId]?.clean()
    }

    override fun afterAll(context: ExtensionContext) {
        logger.info { "[test] cleaning all RDS testbeds" }
        testBeds.values.forEach { it.clean() }
    }

}