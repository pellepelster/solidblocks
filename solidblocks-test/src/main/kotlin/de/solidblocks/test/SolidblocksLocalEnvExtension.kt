package de.solidblocks.test

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class SolidblocksLocalEnvExtension : ParameterResolver, BeforeAllCallback, AfterAllCallback {

    val solidblocksLocalEnv: SolidblocksLocalEnv

    init {
        solidblocksLocalEnv = SolidblocksLocalEnv()
    }

    override fun afterAll(context: ExtensionContext) {
        solidblocksLocalEnv.stop()
    }

    override fun beforeAll(context: ExtensionContext) {
        solidblocksLocalEnv.start()

        if (!solidblocksLocalEnv.bootstrap()) {
            solidblocksLocalEnv.stop()
            throw RuntimeException("failed to provision local environment")
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(SolidblocksLocalEnv::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return solidblocksLocalEnv
    }
}
