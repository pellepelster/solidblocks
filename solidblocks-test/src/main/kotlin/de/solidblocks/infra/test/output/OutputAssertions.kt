package de.solidblocks.infra.test.output

import de.solidblocks.infra.test.CommandRunResult
import io.kotest.matchers.*
import io.kotest.matchers.string.match

fun OutputMatcherResult.shouldMatch(): OutputMatcherResult {
    this.matched shouldBe true
    return this
}

fun OutputMatcherResult.shouldNotMatch(): OutputMatcherResult {
    this.matched shouldBe false
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

fun unmatchedWaitForOutputEqual(size: Int) = Matcher<CommandRunResult> { value ->
    MatcherResult(
        value.unmatchedOutputMatchers.size == size,
        {
            "all wait for output conditions should have matched, but the following expressions were not matched ${
                value.unmatchedOutputMatchers.joinToString(", ") { "'${it.regex}'" }
            }"
        },
        { "all wait for output expressions have matched, but at least $size expressions should not have matched" },
    )
}

fun CommandRunResult.shouldNotHaveUnmatchedWaitForOutput(): CommandRunResult {
    this should unmatchedWaitForOutputEqual(0)
    return this
}

infix fun CommandRunResult.shouldHaveUnmatchedWaitForOutput(expected: Int): CommandRunResult {
    this should unmatchedWaitForOutputEqual(expected)
    return this
}
