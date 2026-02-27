package de.solidblocks.cloud.providers

import de.solidblocks.cloud.configuration.StringConstraints

val HETZNER_INSTANCE_TYPE = StringConstraints(
    options = listOf(
        "cpx21",
        "cpx31",
        "cpx41",
        "cpx51",
        "cax11",
        "cax21",
        "cax31",
        "cax41",
        "ccx13",
        "ccx23",
        "ccx33",
        "ccx43",
        "ccx53",
        "ccx63",
        "cpx12",
        "cpx22",
        "cpx32",
        "cpx42",
        "cpx52",
        "cpx62",
        "cx23",
        "cx33",
        "cx43",
        "cx53"
    )
)

val HETZNER_LOCATIONS = StringConstraints(options = listOf("fsn1", "nbg1", "hel1", "ash", "hil", "sin"))
