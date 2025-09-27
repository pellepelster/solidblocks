package de.solidblocks.hetzner.dns

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant


@OptIn(ExperimentalTime::class)
object MultiFormatInstantDeserializer : KSerializer<kotlin.time.Instant> {

    private val DATE_FORMATTERS =
        arrayOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z z"),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS Z z"),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z"),
        )

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: kotlin.time.Instant) {
        TODO("not implemented")
    }

    override fun deserialize(decoder: Decoder): kotlin.time.Instant {
        val date = decoder.decodeString()

        for (formatter in DATE_FORMATTERS) {
            try {
                return formatter.parse(date).toInstant().toKotlinInstant()
            } catch (e: ParseException) {
            }
        }

        throw RuntimeException(
            "Unparseable date: \"$date\". Supported formats: " +
                    Arrays.stream(DATE_FORMATTERS)
                        .map { obj: SimpleDateFormat -> obj.toPattern() }
                        .collect(Collectors.joining(",")),
        )
    }
}



