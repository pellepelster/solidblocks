package de.solidblocks.utils

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.Instant

class EpochSecondInstantSerializerTest {

    @Serializable
    data class EpochWrapper(
        @Serializable(with = EpochSecondInstantSerializer::class) val timestamp: Instant,
    )

    @Test
    fun `serializes instant to epoch second long`() {
        val instant = Instant.ofEpochSecond(1_700_000_000L)
        val json = Json.encodeToString(EpochWrapper(instant))
        json shouldBe """{"timestamp":1700000000}"""
    }

    @Test
    fun `deserializes epoch second long to instant`() {
        val json = """{"timestamp":1700000000}"""
        val wrapper = Json.decodeFromString<EpochWrapper>(json)
        wrapper.timestamp shouldBe Instant.ofEpochSecond(1_700_000_000L)
    }

    @Test
    fun `round-trips an instant`() {
        val instant = Instant.ofEpochSecond(1_234_567_890L)
        val json = Json.encodeToString(EpochWrapper(instant))
        val decoded = Json.decodeFromString<EpochWrapper>(json)
        decoded.timestamp shouldBe instant
    }

    @Test
    fun `truncates sub-second precision`() {
        val instant = Instant.parse("2024-06-01T12:00:00.999999999Z")
        val json = Json.encodeToString(EpochWrapper(instant))
        val decoded = Json.decodeFromString<EpochWrapper>(json)
        Instant.parse("2024-06-01T12:00:00Z") shouldBe decoded.timestamp
    }
}
