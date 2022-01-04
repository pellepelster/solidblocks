package de.solidblocks.test

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class DevelopmentEnvironmentExtension : ParameterResolver, BeforeAllCallback, AfterAllCallback {

    private val developmentEnvironment: DevelopmentEnvironment = DevelopmentEnvironment()

    override fun afterAll(context: ExtensionContext) {
        developmentEnvironment.stop()
    }

    override fun beforeAll(context: ExtensionContext) {
        developmentEnvironment.start()

        if (!developmentEnvironment.createCloud()) {
            developmentEnvironment.stop()
            throw RuntimeException("failed to provision local environment")
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(DevelopmentEnvironment::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return developmentEnvironment
    }
}
