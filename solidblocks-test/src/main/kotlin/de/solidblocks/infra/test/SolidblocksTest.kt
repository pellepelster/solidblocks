package de.solidblocks.infra.test

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
        log("creating test context with id '$testId' for '${extensionContext.uniqueId}'")
        SolidblocksTestContext(testId)
      }

  override fun afterAll(context: ExtensionContext) {
    contexts.forEach { it.value.afterAll() }
    contexts.forEach { it.value.cleanup() }
  }

  override fun beforeAll(context: ExtensionContext) {
    contexts.forEach { it.value.beforeAll() }
  }

  override fun testFailed(context: ExtensionContext, cause: Throwable?) {
    contexts[context.uniqueId]?.markFailed()
  }
}
