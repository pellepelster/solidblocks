package de.solidblocks.infra.test.assertions

import de.solidblocks.infra.test.command.CommandResult
import de.solidblocks.infra.test.output.OutputMatcherResult
import io.kotest.matchers.*
import io.kotest.matchers.comparables.gt
import io.kotest.matchers.comparables.lt
import io.kotest.matchers.string.match
import kotlin.time.Duration

fun haveExitCode(exitCode: Int) =
    Matcher<CommandResult<*>> { value ->
      MatcherResult(
          value.exitCode == exitCode,
          { "exit code was ${value.exitCode} but we expected $exitCode" },
          { "exit code should not be $exitCode" },
      )
    }

infix fun CommandResult<*>.shouldHaveExitCode(exitCode: Int): CommandResult<*> {
  this should haveExitCode(exitCode)
  return this
}

infix fun CommandResult<*>.runtimeShouldBeLessThan(duration: Duration): CommandResult<*> {
  this.runtime shouldBe lt(duration)
  return this
}

infix fun CommandResult<*>.runtimeShouldBeGreaterThan(duration: Duration): CommandResult<*> {
  this.runtime shouldBe gt(duration)
  return this
}

fun OutputMatcherResult.shouldMatch(): OutputMatcherResult {
  this.matched shouldBe true
  return this
}

fun OutputMatcherResult.shouldNotMatch(): OutputMatcherResult {
  this.matched shouldBe false
  return this
}

infix fun CommandResult<*>.outputShouldMatch(regex: String): CommandResult<*> {
  this.output should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
  return this
}

infix fun CommandResult<*>.stdoutShouldMatch(regex: String): CommandResult<*> {
  this.stdout should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
  return this
}

infix fun CommandResult<*>.stdoutShouldBe(expected: String): CommandResult<*> {
  this.stdout should be(expected)
  return this
}

infix fun CommandResult<*>.stderrShouldBe(expected: String): CommandResult<*> {
  this.stderr should be(expected)
  return this
}

fun CommandResult<*>.stderrShouldBeEmpty(): CommandResult<*> {
  this.stderr should be("")
  return this
}

fun CommandResult<*>.stdoutShouldBeEmpty(): CommandResult<*> {
  this.stdout should be("")
  return this
}

infix fun CommandResult<*>.outputShouldBe(expected: String): CommandResult<*> {
  this.output should be(expected)
  return this
}

infix fun CommandResult<*>.stderrShouldMatch(regex: String): CommandResult<*> {
  this.stderr should match(regex.toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)))
  return this
}
