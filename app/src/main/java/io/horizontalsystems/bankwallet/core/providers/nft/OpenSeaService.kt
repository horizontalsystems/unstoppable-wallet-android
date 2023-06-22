package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.core.managers.APIClient
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

class OpenSeaService(
    hsBaseUrl: String,
    apiKey: String,
    openSeaApiKey: String,
) {
    private val service by lazy {
        APIClient.build(
            baseUrl = "https://api.opensea.io/api/v1/",
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
                "X-API-KEY" to openSeaApiKey
            )
        ).create(OpenSeaApi::class.java)
    }

    private val hsService by lazy {
        APIClient.build("${hsBaseUrl}/v1/nft/", mapOf("apikey" to apiKey))
            .create(HsNftApi::class.java)
    }

    suspend fun allCollections(address: String? = null): List<OpenSeaNftApiResponse.Collection> {
        val collections = mutableListOf<OpenSeaNftApiResponse.Collection>()
        val limit = 300
        do {
            val collectionsResponse = service.collections(address, limit, collections.size)
            collections.addAll(collectionsResponse)
        } while (collectionsResponse.size >= limit)

        return collections
    }

    suspend fun allAssets(address: String): List<OpenSeaNftApiResponse.Asset> {
        val assets = mutableListOf<OpenSeaNftApiResponse.Asset>()
        var cursor: String? = null
        val limit = 30
        do {
            val response = service.assets(owner = address, cursor = cursor, limit = limit)
            assets.addAll(response.assets)
            cursor = response.next
        } while (response.assets.size >= limit && cursor != null)
        return assets
    }

    suspend fun collection(uid: String): OpenSeaNftApiResponse.Collection =
        service.collection(uid).collection

    suspend fun asset(contractAddress: String, tokenId: String): OpenSeaNftApiResponse.Asset =
        service.asset(contractAddress, tokenId)

    suspend fun collectionAssets(uid: String, cursor: String?): OpenSeaNftApiResponse.Assets =
        service.assets(collectionUid = uid, cursor = cursor, limit = 30)

    suspend fun collectionEvents(uid: String, type: String?, cursor: String?): OpenSeaNftApiResponse.Events =
        hsService.events(collectionUid = uid, eventType = type, cursor = cursor)

    suspend fun assetEvents(contractAddress: String, tokenId: String, type: String?, cursor: String?): OpenSeaNftApiResponse.Events =
        hsService.events(contractAddress = contractAddress, tokenId = tokenId, eventType = type, cursor = cursor)

    suspend fun assets(contractAddresses: List<String>, tokenIds: List<String>): OpenSeaNftApiResponse.Assets =
        service.assets(contractAddresses = contractAddresses, tokenIds = tokenIds)
}

interface HsNftApi {

    @GET("events")
    suspend fun events(
        @Query("collection_slug") collectionUid: String? = null,
        @Query("asset_contract_address") contractAddress: String? = null,
        @Query("token_id") tokenId: String? = null,
        @Query("event_type") eventType: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("simplified") simplified: Boolean = true
    ): OpenSeaNftApiResponse.Events
}

interface OpenSeaApi {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String? = null,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("format") format: String = "json"
    ): List<OpenSeaNftApiResponse.Collection>

    @GET("collection/{slug}")
    suspend fun collection(
        @Path("slug") slug: String,
        @Query("format") format: String = "json"
    ): OpenSeaNftApiResponse.CollectionResponse

    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String? = null,
        @Query("collection") collectionUid: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("asset_contract_addresses") contractAddresses: List<String>? = null,
        @Query("token_ids") tokenIds: List<String>? = null,
        @Query("limit") limit: Int? = null,
        @Query("include_orders") includeOrders: Boolean = true,
        @Query("format") format: String = "json"
    ): OpenSeaNftApiResponse.Assets

    @GET("asset/{contractAddress}/{tokenId}")
    suspend fun asset(
        @Path("contractAddress") contractAddress: String,
        @Path("tokenId") tokenId: String,
        @Query("include_orders") includeOrders: Boolean = true
    ): OpenSeaNftApiResponse.Asset
}


object OpenSeaNftApiResponse {
    data class CollectionResponse(val collection: Collection)
    data class Collection(
        val primary_asset_contracts: List<AssetContract>?,
        val slug: String,
        val name: String,
        val description: String?,
        val image_url: String?,
        val large_image_url: String?,
        val external_url: String?,
        val discord_url: String?,
        val twitter_username: String?,
        val dev_seller_fee_basis_points: BigDecimal?,
        val stats: Stats?
    ) {
        data class Links(
            val external_url: String?,
            val discord_url: String?,
            val telegram_url: String?,
            val twitter_username: String?,
            val instagram_username: String?,
            val wiki_url: String?,
        )

        data class Stats(
            val one_day_volume: BigDecimal,
            val one_day_change: BigDecimal,
            val one_day_sales: Int,
            val one_day_average_price: BigDecimal,
            val seven_day_volume: BigDecimal,
            val seven_day_change: BigDecimal,
            val seven_day_sales: Int,
            val seven_day_average_price: BigDecimal,
            val thirty_day_volume: BigDecimal,
            val thirty_day_change: BigDecimal,
            val thirty_day_sales: Int,
            val thirty_day_average_price: BigDecimal,
            val total_volume: BigDecimal?,
            val total_supply: Int,
            val count: Int?,
            val num_owners: Int?,
            val market_cap: BigDecimal,
            val floor_price: BigDecimal?
        )
    }

    data class AssetContract(
        val name: String,
        val address: String,
        val created_date: String,
        val schema_name: String?
    )

    data class Assets(val next: String?, val assets: List<Asset>)

    data class Asset(
        val asset_contract: AssetContract,
        val collection: Collection,
        val token_id: String,
        val name: String?,
        val image_url: String,
        val image_preview_url: String,
        val description: String?,
        val external_link: String?,
        val permalink: String?,
        val traits: List<Trait>?,
        val last_sale: LastSale?,
        val seaport_sell_orders: List<Order>?
    ) {
        val orders: List<Order>
            get() = seaport_sell_orders ?: listOf()

        data class Trait(
            val trait_type: String,
            val value: String,
            val display_type: String?,
            val max_value: Any?,
            val trait_count: Int,
            val order: Any?,
        )

        data class LastSale(val total_price: BigDecimal, val payment_token: PaymentToken) {
            data class PaymentToken(val address: String, val decimals: Int)
        }

        data class Order(
            val expiration_time: Long,
            val protocol_data: ProtocolData,
            val current_price: BigDecimal,
            val side: String,
            val order_type: String
        ) {
            val offer: List<OrderOffer>
                get() = protocol_data.parameters.offer

            val consideration: List<OrderOffer>
                get() = protocol_data.parameters.consideration

            data class ProtocolData(val parameters: Parameters)
            data class Parameters(val offer: List<OrderOffer>, val consideration: List<OrderOffer>)
            data class OrderOffer(val token: String)
        }
    }

    data class Events(val next: String?, val asset_events: List<Event>)

    data class Event(
        val asset: Asset?,
        val event_type: String,
        val event_timestamp: String,
        val total_price: BigDecimal?,
        val payment_token: PaymentToken?
    ) {
        data class PaymentToken(val address: String, val decimals: Int, val eth_price: BigDecimal)
    }
}
