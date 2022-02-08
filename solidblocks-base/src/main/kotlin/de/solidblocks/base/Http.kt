package de.solidblocks.base

import mu.KotlinLogging
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.xbill.DNS.*
import org.xbill.DNS.lookup.LookupSession
import java.net.InetAddress
import java.time.Duration

data class ResolverConfig(val address: String, val session: LookupSession)

val lookupSessions = listOf("8.8.8.8", "1.1.1.1", "208.67.222.222", "208.67.220.220").map {
    val resolver: Resolver = SimpleResolver(it)
    ResolverConfig(it, LookupSession.defaultBuilder().cache(Cache()).resolver(resolver).build())
}

class OkHttpDns : Dns {
    private val logger = KotlinLogging.logger {}

    override fun lookup(hostname: String): List<InetAddress> {
        val config = lookupSessions.shuffled().first()
        try {
            val result = config.session.lookupAsync(Name.fromString(hostname), Type.A).toCompletableFuture().get()
            return result.records.map { InetAddress.getByName(it.rdataToString()) }
        } catch (e: Exception) {
            logger.error { "failed to resolve '${hostname}' using resolver '${config.address}'" }
        }

        return emptyList()
    }
}

fun defaultHttpClient() = defaultHttpClientBuilder().build()

fun defaultHttpClientBuilder() = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(10))
        .dns(OkHttpDns())
        .readTimeout(Duration.ofSeconds(10))
        .connectTimeout(Duration.ofSeconds(10))