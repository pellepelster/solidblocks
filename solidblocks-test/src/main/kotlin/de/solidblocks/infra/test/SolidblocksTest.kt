package de.solidblocks.infra.test

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

public class SolidblocksTest : ParameterResolver, AfterEachCallback {

    private val contexts = mutableMapOf<String, SolidblocksTestContext>()

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ) = parameterContext.parameter.type == SolidblocksTestContext::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ) = contexts.getOrPut(extensionContext.uniqueId) {
        SolidblocksTestContext()
    }

    override fun afterEach(context: ExtensionContext) {
        contexts.forEach {
            it.value.close()
        }
    }

}