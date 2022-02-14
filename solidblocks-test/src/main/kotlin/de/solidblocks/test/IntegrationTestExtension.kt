package de.solidblocks.test

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class IntegrationTestExtension : ParameterResolver, BeforeAllCallback, AfterAllCallback {

    private val integrationTestEnvironment = IntegrationTestEnvironment()

    override fun afterAll(context: ExtensionContext) {
        integrationTestEnvironment.stop()
    }

    override fun beforeAll(context: ExtensionContext) {
        integrationTestEnvironment.start()

        if (!integrationTestEnvironment.createIntegrationTestCloud()) {
            integrationTestEnvironment.stop()
            throw RuntimeException("failed to provision local environment")
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(IntegrationTestEnvironment::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return integrationTestEnvironment
    }
}
