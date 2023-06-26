package de.solidblocks.rds.postgresql.test.extensions

import org.junit.jupiter.api.extension.*


class AwsTestBedExtension : ParameterResolver, AfterEachCallback, BeforeEachCallback {

    val testBeds = mutableMapOf<String, AwsTestBed>()

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
        return createTestBed(context)
    }

    override fun afterEach(context: ExtensionContext) {
        //testBeds[context.uniqueId]?.destroyTestBed()
    }

    override fun beforeEach(context: ExtensionContext) {
        createTestBed(context)
    }

    private fun createTestBed(context: ExtensionContext): AwsTestBed {
        return testBeds.getOrPut(context.uniqueId) {
            AwsTestBed()
        }.also { it.initTestbed() }
    }
}