package de.solidblocks.rds.postgresql.test.extensions

import org.junit.jupiter.api.extension.*

class HetznerTestBedExtension : ParameterResolver, AfterEachCallback, AfterAllCallback {

  private val testBeds = mutableMapOf<String, HetznerTestBed>()

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ): Boolean = parameterContext.parameter.type == HetznerTestBed::class.java

  @Throws(ParameterResolutionException::class)
  override fun resolveParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ): Any {
    logger.info { "[test] creating Hetzner testbed" }
    return testBeds.getOrPut(extensionContext.uniqueId) {
      HetznerTestBed().also { it.initTestbed() }
    }
  }

  override fun afterEach(context: ExtensionContext) {
    logger.info { "[test] cleaning Hetzner testbed '${context.uniqueId}'" }
    testBeds[context.uniqueId]?.clean()
  }

  override fun afterAll(context: ExtensionContext) {
    logger.info { "[test] cleaning all Hetzner testbeds" }
    testBeds.values.forEach { it.clean() }
  }
}
