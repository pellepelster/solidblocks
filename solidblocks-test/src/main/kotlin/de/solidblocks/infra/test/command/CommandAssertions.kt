package de.solidblocks.infra.test.command

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.comparables.gt
import io.kotest.matchers.comparables.lt
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.time.Duration

fun haveExitCode(exitCode: Int) =
    Matcher<CommandRunResult> { value ->
      MatcherResult(
          value.result.exitCode == exitCode,
          { "exit code was ${value.result.exitCode} but we expected $exitCode" },
          { "exit code should not be $exitCode" },
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

infix fun CommandRunResult.runtimeShouldBeGreaterThan(duration: Duration): CommandRunResult {
  this.result.runtime shouldBe gt(duration)
  return this
}
