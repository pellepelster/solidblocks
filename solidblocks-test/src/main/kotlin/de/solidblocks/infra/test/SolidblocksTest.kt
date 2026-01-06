package de.solidblocks.infra.test

import de.solidblocks.utils.logInfo
import java.math.BigInteger
import java.security.MessageDigest
import org.junit.jupiter.api.extension.*

public class SolidblocksTest : ParameterResolver, BeforeAllCallback, AfterAllCallback, TestWatcher {

  private val contexts = mutableMapOf<String, SolidblocksTestContext>()

  override fun supportsParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) = parameterContext.parameter.type == SolidblocksTestContext::class.java

  fun String.toTestId(length: Int): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val alphanumeric = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val base = alphanumeric.length

    val hashBytes = digest.digest(this.toByteArray())
    var number = BigInteger(1, hashBytes)

    val sb = StringBuilder()

    repeat(length) {
      val remainder = (number.mod(BigInteger.valueOf(base.toLong()))).toInt()
      sb.append(alphanumeric[remainder])
      number = number.divide(BigInteger.valueOf(base.toLong()))
    }

    return sb.toString()
  }

  override fun resolveParameter(
      parameterContext: ParameterContext,
      extensionContext: ExtensionContext,
  ) =
      contexts.getOrPut(extensionContext.uniqueId) {
        val testId = extensionContext.uniqueId.toTestId(12)
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

  override fun beforeAll(context: ExtensionContext) {
    allContexts().forEach { it.beforeAll() }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable?) {
    contexts[context.uniqueId]?.markFailed()
  }
}
