package de.solidblocks.infra.test

import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.DecimalFormat
import kotlin.time.Duration


val logger = KotlinLogging.logger {}

val durationFormat = DecimalFormat("000.###")

fun log(duration: Duration, message: String) {
    println("[soliblocks] ${durationFormat.format(duration.inWholeMilliseconds / 1000f)}s ${message}")
}