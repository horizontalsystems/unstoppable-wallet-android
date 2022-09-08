package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.marketkit.models.*
import io.horizontalsystems.marketkit.providers.RetrofitUtils
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

class HsNftProvider(baseUrl: String, apiKey: String) {
    private val service by lazy {
        RetrofitUtils.build("${baseUrl}/v1/nft/", mapOf("apikey" to apiKey))
            .create(HsNftApiV1::class.java)
    }

    suspend fun allCollections(address: String? = null): List<HsNftApiV1Response.Collection> {
        val collections = mutableListOf<HsNftApiV1Response.Collection>()
        var page = 1
        val limit = 300
        do {
            val collectionsResponse = service.collections(address, page, limit)
            collections.addAll(collectionsResponse)
            page++
        } while (collectionsResponse.size >= limit)

        return collections
    }

    suspend fun allAssets(address: String): List<HsNftApiV1Response.Asset> {
        val assets = mutableListOf<HsNftApiV1Response.Asset>()
        var cursor: String? = null
        val limit = 50
        do {
            val response = service.assets(owner = address, cursor = cursor, limit = limit)
            assets.addAll(response.assets)
            cursor = response.cursor.next
        } while (response.assets.size >= limit)
        return assets
    }

    suspend fun collection(uid: String): HsNftApiV1Response.Collection =
        service.collection(uid, includeStatsChart = true)

    suspend fun asset(contractAddress: String, tokenId: String): HsNftApiV1Response.Asset =
        service.asset(contractAddress, tokenId)

    suspend fun collectionAssets(uid: String, cursor: String?): HsNftApiV1Response.Assets =
        service.assets(collectionUid = uid, cursor = cursor, limit = 50)

    suspend fun collectionEvents(uid: String, type: NftEvent.EventType?, cursor: String?): HsNftApiV1Response.Events =
        service.events(collectionUid = uid, eventType = type?.value, cursor = cursor)

    suspend fun assetEvents(contractAddress: String, tokenId: String, type: NftEvent.EventType?, cursor: String?): HsNftApiV1Response.Events =
        service.events(contractAddress = contractAddress, tokenId = tokenId, eventType = type?.value, cursor = cursor)

}

interface HsNftApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String? = null,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): List<HsNftApiV1Response.Collection>

    @GET("collection/{uid}")
    suspend fun collection(
        @Path("uid") uid: String,
        @Query("include_stats_chart") includeStatsChart: Boolean
    ): HsNftApiV1Response.Collection

    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String? = null,
        @Query("collection_uid") collectionUid: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("include_orders") includeOrders: Boolean = true
    ): HsNftApiV1Response.Assets

    @GET("collection/{uid}/stats")
    suspend fun collectionStats(
        @Path("uid") collectionUid: String
    ): HsNftApiV1Response.Collection.Stats

    @GET("asset/{contractAddress}/{tokenId}")
    suspend fun asset(
        @Path("contractAddress") contractAddress: String,
        @Path("tokenId") tokenId: String,
        @Query("include_orders") includeOrders: Boolean = true
    ): HsNftApiV1Response.Asset

    @GET("events")
    suspend fun events(
        @Query("collection_uid") collectionUid: String? = null,
        @Query("asset_contract") contractAddress: String? = null,
        @Query("token_id") tokenId: String? = null,
        @Query("event_type") eventType: String? = null,
        @Query("cursor") cursor: String? = null
    ): HsNftApiV1Response.Events

}


object HsNftApiV1Response {

    data class Collection(
        val asset_contracts: List<Asset.Contract>?,
        val uid: String,
        val name: String,
        val description: String?,
        val image_data: ImageData?,
        val links: Links?,
        val stats: Stats,
        val stats_chart: List<ChartPoint>?
    ) {
        data class ImageData(
            val image_url: String?,
            val featured_image_url: String?,
        )

        data class Links(
            val external_url: String?,
            val discord_url: String?,
            val telegram_url: String?,
            val twitter_username: String?,
            val instagram_username: String?,
            val wiki_url: String?,
        )

        data class Stats(
            val count: Int?,
            val num_owners: Int?,
            val total_supply: Int,
            val one_day_change: BigDecimal,
            val seven_day_change: BigDecimal,
            val thirty_day_change: BigDecimal,
            val one_day_average_price: BigDecimal,
            val seven_day_average_price: BigDecimal,
            val thirty_day_average_price: BigDecimal,
            val floor_price: BigDecimal?,
            val total_volume: BigDecimal?,
            val market_cap: BigDecimal,
            val one_day_volume: BigDecimal,
            val seven_day_volume: BigDecimal,
            val thirty_day_volume: BigDecimal,
            val one_day_sales: Int,
            val seven_day_sales: Int,
            val thirty_day_sales: Int
        )

        data class ChartPoint(
            val timestamp: Long,
            val one_day_volume: BigDecimal?,
            val average_price: BigDecimal?,
            val floor_price: BigDecimal?,
            val one_day_sales: BigDecimal?
        )
    }

    data class Assets(
        val cursor: Cursor,
        val assets: List<Asset>
    )

    data class Asset(
        val contract: Contract,
        val collection_uid: String,
        val token_id: String,
        val name: String?,
        val symbol: String,
        val image_data: ImageData?,
        val description: String?,
        val links: Links?,
        val attributes: List<Attribute>?,
        val markets_data: MarketsData
    ) {
        data class Contract(
            val address: String,
            val type: String,
        )

        data class ImageData(
            val image_url: String?,
            val image_preview_url: String?,
        )

        data class Links(
            val external_link: String?,
            val permalink: String,
        )

        data class Attribute(
            val trait_type: String,
            val value: String,
            val display_type: String?,
            val max_value: Any?,
            val trait_count: Int,
            val order: Any?,
        )

        data class MarketsData(
            val last_sale: LastSale?,
            val sell_orders: List<Order>?,
            val orders: List<Order>?
        ) {
            data class LastSale(
                val total_price: BigDecimal,
                val payment_token: PaymentToken,
            ) {
                data class PaymentToken(
                    val address: String,
                    val decimals: Int
                )
            }

            data class Order(
                val closing_date: String,
                val current_price: BigDecimal,
                val payment_token_contract: PaymentTokenContract,
                val taker: Taker,
                val side: Int,
                val v: Int?,
            ) {
                data class Taker(
                    val address: String
                )

                data class PaymentTokenContract(
                    val address: String,
                    val decimals: Int,
                    val eth_price: BigDecimal
                )
            }
        }
    }

    data class Events(
        val cursor: Cursor,
        val events: List<Event>
    )

    data class Event(
        val asset: Asset?,
        val date: String,
        val type: String,
        val amount: BigDecimal,
        val quantity: String,
        val transaction: Transaction,
        val markets_data: MarketsData
    ) {

        data class Transaction(
            val id: Long,
            val block_hash: String,
            val block_number: Long,
            val timestamp: String,
            val from_account: Account,
            val to_account: Account,
            val transaction_hash: String,
            val transaction_index: String
        ) {
            data class Account(
                val profile_img_url: String,
                val address: String
            )
        }

        data class MarketsData(
            val payment_token: PaymentToken?
        ) {
            data class PaymentToken(
                val symbol: String,
                val address: String,
                val image_url: String?,
                val name: String,
                val decimals: Int,
                val eth_price: BigDecimal,
                val usd_price: BigDecimal
            )
        }

    }

    data class Cursor(val next: String?, val previous: String)

}
