package de.solidblocks.test

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class TestEnvironmentExtension : ParameterResolver, BeforeAllCallback {

    val testEnvironment = TestEnvironment()

    override fun beforeAll(context: ExtensionContext) {
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(TestEnvironment::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return testEnvironment
    }
}
