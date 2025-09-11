package de.solidblocks.cli.hetzner

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object AlphanumericHasher {

    private val base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()

    fun hashToBase62(input: String, desiredLength: Int): String {
        val result = StringBuilder()
        var counter = 0

        while (result.length < desiredLength) {
            val toHash = "$input:$counter"
            val hashBytes = sha256(toHash)
            val base62 = toBase62(hashBytes)
            result.append(base62)
            counter++
        }

        return result.substring(0, desiredLength)
    }

    private fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
    }

    private fun toBase62(bytes: ByteArray): String {
        var number = BigInteger(1, bytes) // Treat bytes as a positive number
        val base = BigInteger.valueOf(62)
        val sb = StringBuilder()

        while (number > BigInteger.ZERO) {
            val remainder = number.mod(base).toInt()
            sb.append(base62Chars[remainder])
            number = number.divide(base)
        }

        return sb.reverse().toString()
    }
}
