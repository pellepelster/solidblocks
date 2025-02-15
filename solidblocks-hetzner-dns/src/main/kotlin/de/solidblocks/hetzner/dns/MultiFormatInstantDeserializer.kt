package de.solidblocks.hetzner.dns

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Arrays
import java.util.stream.Collectors

class MultiFormatInstantDeserializer : StdDeserializer<Instant>(Instant::class.java) {

  override fun deserialize(jp: JsonParser, context: DeserializationContext): Instant {
    val node = jp.codec.readTree<JsonNode>(jp)
    val date = node.textValue()

    for (formatter in DATE_FORMATTERS) {
      try {
        return formatter.parse(date).toInstant()
      } catch (e: ParseException) {}
    }
    throw JsonParseException(
        jp,
        "Unparseable date: \"$date\". Supported formats: " +
            Arrays.stream(DATE_FORMATTERS)
                .map { obj: SimpleDateFormat -> obj.toPattern() }
                .collect(Collectors.joining(",")),
    )
  }

  companion object {
    private val DATE_FORMATTERS =
        arrayOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z z"),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS Z z"),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z z"),
        )
  }
}
