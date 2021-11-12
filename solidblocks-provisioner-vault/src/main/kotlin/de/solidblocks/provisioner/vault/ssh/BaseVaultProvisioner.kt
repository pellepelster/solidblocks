package de.solidblocks.provisioner.vault.ssh

import org.joda.time.Instant
import org.joda.time.MutablePeriod
import org.joda.time.format.PeriodFormatterBuilder
import java.util.*

val COMPARE_VAULT_TTL: (String, String) -> Boolean = { expectedValue, actualValue ->
    val expectedPeriod = MutablePeriod()
    val actualPeriod = MutablePeriod()

    val parser = PeriodFormatterBuilder()
        .appendHours().appendSuffix("h")
        .appendMinutes().appendSuffix("m")
        .appendSeconds()
        .toParser()

    parser.parseInto(expectedPeriod, expectedValue, 0, Locale.getDefault())
    parser.parseInto(actualPeriod, actualValue, 0, Locale.getDefault())
    expectedPeriod.toDurationFrom(Instant.now()).standardSeconds == actualPeriod.toDurationFrom(Instant.now()).standardSeconds
}
