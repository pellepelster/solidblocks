package de.solidblocks.infra.test

import org.junit.jupiter.api.extension.*

public class SolidblocksTest : ParameterResolver, AfterAllCallback, TestWatcher {

  private val contexts = mutableMapOf<String, SolidblocksTestContext>()

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) = parameterContext.parameter.type == SolidblocksTestContext::class.java

  override fun resolveParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) = contexts.getOrPut(extensionContext.uniqueId) { SolidblocksTestContext() }

  override fun afterAll(context: ExtensionContext) {
    contexts.forEach { it.value.afterAll() }
    contexts.forEach { it.value.cleanup() }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable?) {
    contexts.get(context.uniqueId)?.markFailed()
  }
}
