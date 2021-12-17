package de.solidblocks.test

import de.solidblocks.cloud.config.SolidblocksDatabase
import de.solidblocks.test.TestConstants.TEST_DB_JDBC_URL
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class SolidblocksTestDatabaseExtension : ParameterResolver, BeforeAllCallback {

    val solidblocksDatabase: SolidblocksDatabase

    init {
        solidblocksDatabase = SolidblocksDatabase(TEST_DB_JDBC_URL)
    }

    override fun beforeAll(context: ExtensionContext) {
        solidblocksDatabase.ensureDBSchema()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(SolidblocksDatabase::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return solidblocksDatabase
    }
}
