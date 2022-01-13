package de.solidblocks.vault.http

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.vault.VaultCaCertificateManager
import de.solidblocks.vault.VaultCertificateManager
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.util.PemUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MtlsHttpClient(
    val baseAddress: String,
    val vaultCertificateManager: VaultCertificateManager,
    val vaultCaCertificateManager: VaultCaCertificateManager
) {

    val objectMapper = jacksonObjectMapper()

    val client: OkHttpClient

    init {
        val caCertificate = vaultCaCertificateManager.waitForCaCertificate()

        val k = vaultCertificateManager.waitForCertificate()

        val key = PemUtils.loadPrivateKey(k.privateKeyRaw.byteInputStream())

        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(key, null, k.public)
            .withTrustMaterial(caCertificate.caCertificate).build()

        client = OkHttpClient.Builder().sslSocketFactory(sslFactory.sslSocketFactory, sslFactory.trustManager.get())
            .build()
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T> get(path: String): HttpResponse<T> {
        val type = objectMapper.typeFactory.constructType(T::class.java)

        val request = Request.Builder().url("$baseAddress/$path").build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, objectMapper.readValue(response.body?.bytes(), type))
    }

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T> post(path: String, data: Any): HttpResponse<T> {
        val type = objectMapper.typeFactory.constructType(T::class.java)

        val request = Request.Builder()
            .post(jacksonObjectMapper().writeValueAsString(data).toRequestBody("application/json".toMediaTypeOrNull()))
            .url("$baseAddress/$path").build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, objectMapper.readValue(response.body?.bytes(), type))
    }

    fun post(path: String): HttpResponse<Any> {
        val request =
            Request.Builder().post("".toRequestBody("application/json".toMediaTypeOrNull())).url("$baseAddress/$path")
                .build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, null)
    }
}
