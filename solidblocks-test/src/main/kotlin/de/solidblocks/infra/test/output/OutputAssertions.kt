package de.solidblocks.infra.test.output

import de.solidblocks.infra.test.command.CommandRunResult
import io.kotest.matchers.be
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
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

fun CommandRunResult.stderrShouldBeEmpty(): CommandRunResult {
    this.result.stderr should be("")
    return this
}

fun CommandRunResult.stdoutShouldBeEmpty(): CommandRunResult {
    this.result.stdout should be("")
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

