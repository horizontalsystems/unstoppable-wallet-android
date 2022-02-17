package io.horizontalsystems.bankwallet.modules.opensea

import io.horizontalsystems.bankwallet.core.managers.APIClient
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

object OpenSeaModule {
    private val apiURL = "https://api.opensea.io/api/v1/"

    val apiServiceV1 = APIClient.retrofit(apiURL, 60)
        .create(OpenSeaApiV1::class.java)
}

object OpenSeaApiV1Response {
    data class Collection(
        val slug: String,
        val name: String,
        val image_url: String,
        val stats: Stats
    ) {
        data class Stats(
            val seven_day_average_price: BigDecimal,
            val thirty_day_average_price: BigDecimal,
        )
    }

    data class Asset(
        val token_id: String,
        val name: String,
        val image_url: String,
        val image_preview_url: String,
        val description: String?,
        val collection: Collection,
        val last_sale: LastSale?,
        val asset_contract: AssetContract,
    ) {
        data class Collection(
            val slug: String,
        )

        data class LastSale(
            val total_price: BigInteger,
            val payment_token: PaymentToken,
        ) {
            data class PaymentToken(
                val address: String,
                val decimals: Int,
            )
        }

        data class AssetContract(
            val address: String,
            val schema_name: String,
        )
    }

    data class Assets(
        val assets: List<Asset>
    )
}

interface OpenSeaApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : List<OpenSeaApiV1Response.Collection>

    @Headers(
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    )
    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String,
//        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : OpenSeaApiV1Response.Assets
}
