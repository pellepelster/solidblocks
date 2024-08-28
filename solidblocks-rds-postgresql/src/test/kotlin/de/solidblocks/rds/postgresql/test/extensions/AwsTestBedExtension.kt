package de.solidblocks.rds.postgresql.test.extensions

import org.junit.jupiter.api.extension.*

class AwsTestBedExtension : ParameterResolver, AfterEachCallback, AfterAllCallback {

  private val testBeds = mutableMapOf<String, AwsTestBed>()

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ): Boolean = parameterContext.parameter.type == AwsTestBed::class.java

  @Throws(ParameterResolutionException::class)
  override fun resolveParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ): Any {
    logger.info { "[test] creating AWS testbed" }
    return testBeds.getOrPut(extensionContext.uniqueId) { AwsTestBed().also { it.initTestbed() } }
  }

  override fun afterEach(context: ExtensionContext) {
    logger.info { "[test] cleaning AWS testbed '${context.uniqueId}'" }
    testBeds[context.uniqueId]?.clean()
  }

  override fun afterAll(context: ExtensionContext) {
    logger.info { "[test] cleaning all AWS testbeds" }
    testBeds.values.forEach { it.clean() }
  }
}
