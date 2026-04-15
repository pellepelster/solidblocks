package de.solidblocks.hetzner.cloud.resources

import de.solidblocks.hetzner.cloud.HetznerApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Price(val net: String, val gross: String)

@Serializable
data class PricingImageResponse(@SerialName("price_per_gb_month") val pricePerGbMonth: Price)

@Serializable
data class PricingTrafficResponse(@SerialName("price_per_tb") val pricePerTb: Price)

@Serializable
data class PricingVolumeResponse(@SerialName("price_per_gb_month") val pricePerGbMonth: Price)

@Serializable
data class PricingServerBackupResponse(val percentage: String)

@Serializable
data class PricingFloatingIpTypePrice(val location: String, @SerialName("price_monthly") val priceMonthly: Price)

@Serializable
data class PricingFloatingIpTypeResponse(val type: String, val prices: List<PricingFloatingIpTypePrice>)

@Serializable
data class PricingPrimaryIpTypePrice(
    val location: String,
    @SerialName("price_hourly") val priceHourly: Price,
    @SerialName("price_monthly") val priceMonthly: Price,
)

@Serializable
data class PricingPrimaryIpResponse(val type: String, val prices: List<PricingPrimaryIpTypePrice>)

@Serializable
data class PricingServerTypePrice(
    val location: String,
    @SerialName("price_hourly") val priceHourly: Price,
    @SerialName("price_monthly") val priceMonthly: Price,
    @SerialName("included_traffic") val includedTraffic: Long,
    @SerialName("price_per_tb_traffic") val pricePerTbTraffic: Price,
)

@Serializable
data class PricingServerTypeResponse(val id: Long, val name: String, val prices: List<PricingServerTypePrice>)

@Serializable
data class PricingLoadBalancerTypePrice(
    val location: String,
    @SerialName("price_hourly") val priceHourly: Price,
    @SerialName("price_monthly") val priceMonthly: Price,
    @SerialName("included_traffic") val includedTraffic: Long,
    @SerialName("price_per_tb_traffic") val pricePerTbTraffic: Price,
)

@Serializable
data class PricingLoadBalancerTypeResponse(val id: Long, val name: String, val prices: List<PricingLoadBalancerTypePrice>)

@Serializable
data class PricingResponse(
    val currency: String,
    @SerialName("vat_rate") val vatRate: String,
    val image: PricingImageResponse,
    @SerialName("floating_ips") val floatingIps: List<PricingFloatingIpTypeResponse> = emptyList(),
    @SerialName("primary_ips") val primaryIps: List<PricingPrimaryIpResponse> = emptyList(),
    val traffic: PricingTrafficResponse? = null,
    @SerialName("server_backup") val serverBackup: PricingServerBackupResponse,
    @SerialName("server_types") val serverTypes: List<PricingServerTypeResponse>,
    @SerialName("load_balancer_types") val loadBalancerTypes: List<PricingLoadBalancerTypeResponse>,
    val volume: PricingVolumeResponse,
)

@Serializable
data class PricingResponseWrapper(val pricing: PricingResponse)

class HetznerPricingApi(private val api: HetznerApi) {
    suspend fun get(): PricingResponse = api.get<PricingResponseWrapper>("v1/pricing")?.pricing
        ?: throw RuntimeException("failed to get pricing")
}
