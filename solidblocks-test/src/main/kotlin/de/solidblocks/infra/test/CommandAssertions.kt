package de.solidblocks.infra.test

import io.kotest.matchers.*
import io.kotest.matchers.comparables.gt
import io.kotest.matchers.comparables.lt
import io.kotest.matchers.string.match
import kotlin.time.Duration

fun haveExitCode(exitCode: Int) = Matcher<CommandRunResult> { value ->
    MatcherResult(
        value.result.exitCode == exitCode,
        { "exit code was ${value.result.exitCode} but we expected $exitCode" },
        { "exit code should not be $exitCode" },
    )
}

fun unmatchedWaitForOutputEqual(size: Int) = Matcher<CommandRunResult> { value ->
    MatcherResult(
        value.unmatchedWaitForMatchers.size == size,
        {
            "all wait for output conditions should have matched, but the following expressions were not matched ${
                value.unmatchedWaitForMatchers.joinToString(", ") { "'${it.regex}'" }
            }"
        },
        { "all wait for output expressions have matched, but at least $size expressions should not have matched" },
    )
}

infix fun CommandRunResult.shouldHaveExitCode(exitCode: Int): CommandRunResult {
    this should haveExitCode(exitCode)
    return this
}

infix fun CommandRunResult.runtimeShouldBeLessThan(duration: Duration): CommandRunResult {
    this.result.runtime shouldBe lt(duration)
    return this
}

fun CommandRunResult.shouldNotHaveUnmatchedWaitForOutput(): CommandRunResult {
    this should unmatchedWaitForOutputEqual(0)
    return this
}

infix fun CommandRunResult.shouldHaveUnmatchedWaitForOutput(expected: Int): CommandRunResult {
    this should unmatchedWaitForOutputEqual(expected)
    return this
}

infix fun CommandRunResult.runtimeShouldBeGreaterThan(duration: Duration): CommandRunResult {
    this.result.runtime shouldBe gt(duration)
    return this
}

infix fun CommandRunResult.outputShouldMatch(regex: String): CommandRunResult {
    this.result.output should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
    return this
}

infix fun CommandRunResult.stdoutShouldMatch(regex: String): CommandRunResult {
    this.result.stdout should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
    return this
}

infix fun CommandRunResult.stdoutShouldBe(expected: String): CommandRunResult {
    this.result.stdout should be(expected)
    return this
}

infix fun CommandRunResult.stderrShouldBe(expected: String): CommandRunResult {
    this.result.stderr should be(expected)
    return this
}

infix fun CommandRunResult.outputShouldBe(expected: String): CommandRunResult {
    this.result.output should be(expected)
    return this
}

infix fun CommandRunResult.stderrShouldMatch(regex: String): CommandRunResult {
    this.result.stderr should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
    return this
}

