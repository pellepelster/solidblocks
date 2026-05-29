package de.solidblocks.cloud.utils

import de.solidblocks.cloud.utils.ResultScope.bind
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun testFlatMapChainsOnSuccess() {
        val result = Success(2).flatMap { Success(it * 3) }
        result.shouldBeTypeOf<Success<Int>>().data shouldBe 6
    }

    @Test
    fun testFlatMapShortCircuitsAndPreservesCause() {
        val cause = RuntimeException("boom")
        val result = Error<Int>("failed", cause).flatMap { Success(it * 3) }

        val error = result.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "failed"
        error.cause shouldBe cause
    }

    @Test
    fun testFold() {
        Success(1).fold(onError = { "err" }, onSuccess = { "ok-$it" }) shouldBe "ok-1"
        Error<Int>("nope").fold(onError = { it.error }, onSuccess = { "ok" }) shouldBe "nope"
    }

    @Test
    fun testGetOrElse() {
        Success(5).getOrElse { -1 } shouldBe 5
        Error<Int>("nope").getOrElse { -1 } shouldBe -1
    }

    @Test
    fun testOnErrorRunsOnlyForError() {
        var seen: String? = null
        Success(1).onError { seen = it.error }
        seen shouldBe null

        Error<Int>("bad").onError { seen = it.error }
        seen shouldBe "bad"
    }

    @Test
    fun testRetypePreservesMessageAndCause() {
        val cause = IllegalStateException("x")
        val retyped: Error<String> = Error<Int>("msg", cause).retype()
        retyped.error shouldBe "msg"
        retyped.cause shouldBe cause
    }

    @Test
    fun testCatchingResultCapturesCause() {
        val result = catchingResult<Int> { throw IllegalArgumentException("bad arg") }
        val error = result.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "bad arg"
        error.cause.shouldBeTypeOf<IllegalArgumentException>()
    }

    @Test
    fun testResultBindUnwrapsSuccess() {
        val result = result {
            val a = Success(2).bind()
            val b = Success(3).bind()
            a + b
        }
        result.shouldBeTypeOf<Success<Int>>().data shouldBe 5
    }

    @Test
    fun testResultBindShortCircuitsOnFirstError() {
        var reachedSecond = false
        val result = result {
            Success(1).bind()
            Error<Int>("stop", RuntimeException("cause")).bind()
            reachedSecond = true
            99
        }

        reachedSecond shouldBe false
        val error = result.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "stop"
        error.cause.shouldBeTypeOf<RuntimeException>()
    }
}
