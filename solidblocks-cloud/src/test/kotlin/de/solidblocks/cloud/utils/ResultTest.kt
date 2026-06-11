package de.solidblocks.cloud.utils

import de.solidblocks.cloud.utils.ResultScope.bind
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun `flat map chains on success`() {
        val result = Success(2).flatMap { Success(it * 3) }
        result.shouldBeTypeOf<Success<Int>>().data shouldBe 6
    }

    @Test
    fun `flat map short circuits and preserves cause`() {
        val cause = RuntimeException("boom")
        val result = Error<Int>("failed", cause).flatMap { Success(it * 3) }

        val error = result.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "failed"
        error.cause shouldBe cause
    }

    @Test
    fun `fold`() {
        Success(1).fold(onError = { "err" }, onSuccess = { "ok-$it" }) shouldBe "ok-1"
        Error<Int>("nope").fold(onError = { it.error }, onSuccess = { "ok" }) shouldBe "nope"
    }

    @Test
    fun `get or else`() {
        Success(5).getOrElse { -1 } shouldBe 5
        Error<Int>("nope").getOrElse { -1 } shouldBe -1
    }

    @Test
    fun `on error runs only for error`() {
        var seen: String? = null
        Success(1).onError { seen = it.error }
        seen shouldBe null

        Error<Int>("bad").onError { seen = it.error }
        seen shouldBe "bad"
    }

    @Test
    fun `retype preserves message and cause`() {
        val cause = IllegalStateException("x")
        val retyped: Error<String> = Error<Int>("msg", cause).retype()
        retyped.error shouldBe "msg"
        retyped.cause shouldBe cause
    }

    @Test
    fun `catching result captures cause`() {
        val result = catchingResult<Int> { throw IllegalArgumentException("bad arg") }
        val error = result.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "bad arg"
        error.cause.shouldBeTypeOf<IllegalArgumentException>()
    }

    @Test
    fun `result bind unwraps success`() {
        val result = result {
            val a = Success(2).bind()
            val b = Success(3).bind()
            a + b
        }
        result.shouldBeTypeOf<Success<Int>>().data shouldBe 5
    }

    @Test
    fun `result bind short circuits on first error`() {
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
