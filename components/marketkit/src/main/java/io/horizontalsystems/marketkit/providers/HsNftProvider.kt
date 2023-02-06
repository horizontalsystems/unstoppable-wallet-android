package io.horizontalsystems.marketkit.providers

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal

class HsNftProvider(baseUrl: String, apiKey: String) {

    private val service by lazy {
        RetrofitUtils.build("${baseUrl}/v1/nft/", mapOf("apikey" to apiKey))
            .create(HsNftApiV1::class.java)
    }

    suspend fun topCollections(): List<TopCollectionRaw> {
        val collections = mutableListOf<TopCollectionRaw>()
        val collectionsResponse = service.collections()
        collections.addAll(collectionsResponse)

        return collections
    }

}

interface HsNftApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("simplified") simplified: Boolean = true,
    ): List<TopCollectionRaw>

}


data class TopCollectionRaw(
    @SerializedName("blockchain_uid")
    val blockchainUid: String,
    @SerializedName("opensea_uid")
    val providerUid: String,
    val name: String,
    @SerializedName("thumbnail_url")
    val thumbnailImageUrl: String?,
    @SerializedName("floor_price")
    val floorPrice: BigDecimal?,
    @SerializedName("volume_1d")
    val volume1d: BigDecimal?,
    @SerializedName("change_1d")
    val change1d: BigDecimal?,
    @SerializedName("volume_7d")
    val volume7d: BigDecimal?,
    @SerializedName("change_7d")
    val change7d: BigDecimal?,
    @SerializedName("volume_30d")
    val volume30d: BigDecimal?,
    @SerializedName("change_30d")
    val change30d: BigDecimal?
)
