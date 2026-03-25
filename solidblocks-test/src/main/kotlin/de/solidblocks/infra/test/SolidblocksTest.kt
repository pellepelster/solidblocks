package de.solidblocks.infra.test

import de.solidblocks.utils.logInfo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.TestWatcher

public class SolidblocksTest : ParameterResolver, AfterAllCallback, TestWatcher {

  private val contexts = mutableMapOf<String, SolidblocksTestContext>()

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) = parameterContext.parameter.type == SolidblocksTestContext::class.java

  override fun resolveParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) =
      contexts.getOrPut(extensionContext.uniqueId) {
        val testId = extensionContext.uniqueId.generateTestId()
        logInfo("creating test context with id '$testId' for '${extensionContext.uniqueId}'")
        SolidblocksTestContext(testId)
      }

  fun allContexts(): List<TestContext> {
    val allContexts = mutableListOf<TestContext>()

    contexts.values.forEach { allContexts(allContexts, it) }

    return allContexts.sortedByDescending { it.order }.toList()
  }

  fun allContexts(allContexts: MutableList<TestContext>, context: TestContext) {
    allContexts.add(context)

    context.testContexts.forEach {
      allContexts.addAll(it.testContexts)
      allContexts(allContexts, it)
    }
  }

  override fun afterAll(context: ExtensionContext) {
    allContexts().forEach { it.afterAll() }
    allContexts().forEach { it.cleanUp() }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable?) {
    contexts[context.uniqueId]?.markFailed()
  }
}
