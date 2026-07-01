package de.solidblocks.cloud.api

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class ResultExtensionsTest {

    @Test
    fun `has error is true when any result is an error`() {
        listOf(Success(1), Error<Int>("boom")).hasError() shouldBe true
        listOf(Success(1), Success(2)).hasError() shouldBe false
        emptyList<Result<Int>>().hasError() shouldBe false
    }

    @Test
    fun `aggregate error message joins all error messages`() {
        listOf(Success(1), Error<Int>("a"), Error<Int>("b")).aggregateErrorMessage() shouldBe "a, b"
        listOf(Success(1)).aggregateErrorMessage() shouldBe ""
    }

    @Test
    fun `aggregate returns the block result when there are no errors`() {
        val result = listOf(Success(1), Success(2)).aggregate { "ok" }
        result.shouldBeTypeOf<Success<String>>().data shouldBe "ok"
    }

    @Test
    fun `aggregate returns the combined error message when any result failed`() {
        var blockCalled = false
        val result = listOf(Success(1), Error<Int>("a"), Error<Int>("b")).aggregate {
            blockCalled = true
            "ok"
        }

        blockCalled shouldBe false
        result.shouldBeTypeOf<Error<String>>().error shouldBe "a, b"
    }

    @Test
    fun `map success keeps only the data of successful results`() {
        listOf(Success(1), Error<Int>("x"), Success(2)).mapSuccess() shouldContainExactly listOf(1, 2)
    }

    @Test
    fun `map transforms success and preserves error with cause`() {
        Success(2).map { it * 3 }.shouldBeTypeOf<Success<Int>>().data shouldBe 6

        val cause = RuntimeException("boom")
        val error = Error<Int>("failed", cause).map { it * 3 }.shouldBeTypeOf<Error<Int>>()
        error.error shouldBe "failed"
        error.cause shouldBe cause
    }
}
