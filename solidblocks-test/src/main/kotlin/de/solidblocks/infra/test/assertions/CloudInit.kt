package de.solidblocks.infra.test.assertions

import de.solidblocks.infra.test.cloudinit.CloudInitResultWrapper
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should

fun cloudInitSuccess() =
    Matcher<CloudInitResultWrapper> { value ->
      MatcherResult(
          !value.hasErrors,
          {
            "cloud init was expected to finish without errors but returned ${value.allErrors.joinToString(", ")}"
          },
          { "cloud init was expected to finish with errors but non were returned" },
      )
    }

fun cloudInitResultExists() =
    Matcher<CloudInitResultWrapper?> { value ->
      MatcherResult(
          value != null,
          { "cloud init result was expected but not found" },
          { "cloud init result was not expected but found" },
      )
    }

fun CloudInitResultWrapper?.shouldBeSuccess(): CloudInitResultWrapper? {
  if (this == null) {
    this should cloudInitResultExists()
  } else {
    this should cloudInitSuccess()
  }

  return this
}
