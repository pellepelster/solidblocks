package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stderrShouldMatch
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.LogLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
public class LogTest {

    @RepeatedTest(value = 10)
    fun testLogMessages(testContext: SolidblocksTestContext) {
        val result =
            testContext
                .local()
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("log.sh"))
                .step("log_info 'some info'")
                .step("log_success 'some success'")
                .step("log_warning 'some warning'")
                .step("log_error 'some error'")
                .step("log_debug 'some debug'")
                .run()

        assertSoftly(result) {
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[  success\\] some success.*"
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[  warning\\] some warning.*"
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[    debug\\] some debug.*"
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[    error\\] some error.*"
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[     info\\] some info.*"
        }
    }

    @Test
    fun testLibrarySource() {
        LogLibrary.source() shouldContain "log_success"
    }

    @Test
    fun testLogDie(testContext: SolidblocksTestContext) {
        val result =
            testContext
                .local()
                .script()
                .assertSteps(false)
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("log.sh"))
                .step("log_die 'fatal message'")
                .run()

        assertSoftly(result) {
            it shouldHaveExitCode 4
            it stderrShouldMatch ".*${Calendar.getInstance().get(Calendar.YEAR)}-.* UTC \\[emergency\\] fatal message.*"
        }
    }
}
