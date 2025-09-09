package de.solidblocks.cli.hetzner

import java.security.MessageDigest

class HetznerLabels(hetznerLabels: Map<String, String> = HashMap()) {

    data class HashComparisonResult(val matches: Boolean, val expectedValue: String, val actualValue: String?)

    private val labels: HashMap<String, String> = HashMap()

    private val delimiter = "_"

    private val MAX_VALUE_LENGTH = 62

    private val MAX_TOTAL_VALUE_LENGTH = MAX_VALUE_LENGTH * 2

    init {
        this.labels.putAll(hetznerLabels)
    }

    fun labels(): Map<String, String> {
        return safeLabels()
    }

    fun rawLabels(): Map<String, String> {
        return this.labels
    }

    fun addLabel(key: String, value: String) {
        safeStore(key, value)
    }

    fun addHashedLabel(key: String, value: String) {
        safeStore(key, hashString(value))
    }

    fun hashLabelMatches(key: String, value: String): HashComparisonResult {
        val actualValue = this.safeLabels()[key]
        val expectedValue = hashString(value)

        return HashComparisonResult(actualValue == expectedValue, expectedValue, actualValue)
    }

    private fun safeLabels(): Map<String, String> {

        data class IndexedLabelKey(val key: String, val index: Int)

        val labelsSortedByKeyAndIndex = labels.entries
            .map { it.key.split(delimiter) to it.value }
            .map { (IndexedLabelKey(it.first[0], it.first.getOrElse(1) { "0" }.toInt())) to it.second }
            .sortedWith(compareBy({ it.first.key }, { it.first.index }))

        val map = HashMap<String, String>()

        labelsSortedByKeyAndIndex.forEach {
            if (map[it.first.key] == null) {
                map[it.first.key] = it.second
            } else {
                map[it.first.key] = "${map[it.first.key]}${it.second}"
            }
        }

        return map
    }

    private fun safeStore(key: String, value: String) {

        if (key.contains(delimiter)) {
            throw RuntimeException("keys containing '${delimiter}' are not supported")
        }

        if (value.length > MAX_TOTAL_VALUE_LENGTH) {
            throw RuntimeException("labels values longer than $MAX_TOTAL_VALUE_LENGTH are not supported")
        }

        if (value.length >= MAX_VALUE_LENGTH) {
            value.chunked(MAX_VALUE_LENGTH).forEachIndexed { index, s ->
                this.labels["${key}${delimiter}${index}"] = s
            }
        } else {
            this.labels[key] = value
        }
    }

}

private val messageDigest = MessageDigest.getInstance("SHA-256")

fun hashString(input: String): String {
    return messageDigest.digest(input.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }
}
