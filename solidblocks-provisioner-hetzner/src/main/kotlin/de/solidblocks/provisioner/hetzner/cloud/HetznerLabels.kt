package de.solidblocks.provisioner.hetzner.cloud

import java.security.MessageDigest

class HetznerLabels(hetznerLabels: Map<String, String> = HashMap()) {

    val labels: HashMap<String, String> = HashMap()

    init {
        hetznerLabels.forEach { (key, value) ->
            this.labels[key] = value
        }
    }

    private val messageDigest = MessageDigest
        .getInstance("SHA-256")

    private val labelPrefix = "solidblocks"

    fun labels(): Map<String, String> {
        return labels
    }

    fun addLabel(key: String, value: String) {
        this.labels["$labelPrefix/$key"] = value
    }

    fun addHashLabel(key: String, value: String) {
        this.labels["$labelPrefix/$key"] = hetznerLabelValueHashString(value)
    }

    fun hashLabelMatches(key: String, value: String): Boolean {
        return this.labels["$labelPrefix/$key"] == hetznerLabelValueHashString(value)
    }

    private fun hetznerLabelValueHashString(input: String): String {
        return hashString(input).subSequence(0, 62).toString()
    }

    private fun hashString(input: String): String {
        return messageDigest
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}
