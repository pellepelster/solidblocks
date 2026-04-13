package de.solidblocks.utils

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant

class Iso8601InstantSerializerTest {

    @Serializable
    data class Iso8601Wrapper(
        @Serializable(with = Iso8601InstantSerializer::class) val timestamp: Instant,
    )

    @Test
    fun `serializes instant to ISO-8601 string`() {
        val instant = Instant.parse("2023-11-14T22:13:20Z")
        val json = Json.encodeToString(Iso8601Wrapper(instant))
        json shouldBe """{"timestamp":"2023-11-14T22:13:20Z"}"""
    }

    @Test
    fun `ISO-8601 string to instant`() {
        val json = """{"timestamp":"2023-11-14T22:13:20Z"}"""
        val wrapper = Json.decodeFromString<Iso8601Wrapper>(json)
        wrapper.timestamp shouldBe Instant.parse("2023-11-14T22:13:20Z")
    }

    @Test
    fun `round-trips an instant`() {
        val instant = Instant.parse("2024-06-01T12:00:00Z")
        val json = Json.encodeToString(Iso8601Wrapper(instant))
        val decoded = Json.decodeFromString<Iso8601Wrapper>(json)
        decoded.timestamp shouldBe instant
    }

    @Test
    fun `preserves sub-second precision`() {
        val instant = Instant.parse("2024-06-01T12:00:00.123456789Z")
        val json = Json.encodeToString(Iso8601Wrapper(instant))
        val decoded = Json.decodeFromString<Iso8601Wrapper>(json)
        decoded.timestamp shouldBe instant
    }
}
