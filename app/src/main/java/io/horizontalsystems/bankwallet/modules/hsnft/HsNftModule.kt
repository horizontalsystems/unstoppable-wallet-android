package io.horizontalsystems.bankwallet.modules.hsnft

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.*
import io.horizontalsystems.marketkit.models.CoinType
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

object HsNftModule {
    private val apiURL = App.appConfigProvider.marketApiBaseUrl + "/v1/nft/"

    val apiServiceV1: HsNftApiV1 = APIClient.retrofit(apiURL, 60)
        .create(HsNftApiV1::class.java)
}

object HsNftApiV1Response {
    data class Collection(
        val uid: String,
        val name: String,
        val description: String?,
        val asset_contracts: List<Asset.Contract>,
        val image_data: ImageData?,
        val links: Links?,
        val stats: Stats,
        val stats_chart: List<ChartPoint>
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
            val seven_day_average_price: BigDecimal,
            val thirty_day_average_price: BigDecimal,
            val floor_price: BigDecimal?,
            val average_price: BigDecimal,
            val num_owners: Int,
            val total_supply: Int,
            val total_volume: BigDecimal,
            val one_day_volume: BigDecimal,
            val one_day_change: BigDecimal,
            val one_day_sales: Int,
            val one_day_average_price: BigDecimal,
            val seven_day_volume: BigDecimal,
            val seven_day_change: BigDecimal,
            val thirty_day_volume: BigDecimal,
            val thirty_day_change: BigDecimal,
        )

        data class ChartPoint(
            val timestamp: Long,
            val one_day_volume: BigDecimal,
            val average_price: BigDecimal,
            val floor_price: BigDecimal?,
            val one_day_sales: Int
        )
    }

    data class Events(
        val cursor: Cursor,
        val events: List<Event>
    )

    data class Event(
        val asset: Asset,
        val date: String,
        val type: String,
        val amount: BigInteger,
        val quantity: String,
        val transaction: Transaction,
        val markets_data: MarketsData
    )

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

    data class Assets(
        val cursor: Cursor,
        val assets: List<Asset>
    )

    data class Cursor(val next: String?, val previous: String)
    data class Asset(
        val token_id: String,
        val name: String?,
        val symbol: String,
        val contract: Contract,
        val collection_uid: String,
        val description: String?,
        val image_data: ImageData?,
        val links: Links?,
        val attributes: List<Attribute>,
        val markets_data: MarketsData,
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
                val total_price: BigInteger,
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
}

data class AssetOrder(
    val closingDate: Date?,
    val price: NftAssetPrice?,
    val emptyTaker: Boolean,
    val side: Int,
    val v: Int?,
    val ethValue: BigDecimal
)

class HsNftApiProvider : INftApiProvider {
    private val zeroAddress = "0x0000000000000000000000000000000000000000"

    override suspend fun getCollectionRecords(
        address: Address,
        account: Account
    ) = fetchCollections(address).map { collectionResponse ->
        NftCollectionRecord(
            accountId = account.id,
            uid = collectionResponse.uid,
            name = collectionResponse.name,
            imageUrl = collectionResponse.image_data?.image_url,
            totalSupply = collectionResponse.stats.total_supply,
            averagePrice7d = NftAssetPrice(
                getCoinTypeId(zeroAddress),
                collectionResponse.stats.seven_day_average_price
            ),
            averagePrice30d = NftAssetPrice(
                getCoinTypeId(zeroAddress),
                collectionResponse.stats.thirty_day_average_price
            ),
            floorPrice = collectionResponse.stats.floor_price?.let {
                NftAssetPrice(getCoinTypeId(zeroAddress), it)
            },
            links = collectionResponse.links
        )
    }

    override suspend fun getAssetRecords(
        address: Address,
        account: Account
    ) = fetchAssets(address).map { assetResponse ->
        NftAssetRecord(
            accountId = account.id,
            collectionUid = assetResponse.collection_uid,
            tokenId = assetResponse.token_id,
            name = assetResponse.name,
            imageUrl = assetResponse.image_data?.image_url,
            imagePreviewUrl = assetResponse.image_data?.image_preview_url,
            description = assetResponse.description,
            onSale = assetResponse.markets_data.sell_orders?.isNotEmpty() ?: false,
            lastSale = assetResponse.markets_data.last_sale?.let { last_sale ->
                NftAssetPrice(
                    getCoinTypeId(last_sale.payment_token.address),
                    BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                )
            },
            contract = NftAssetContract(
                assetResponse.contract.address,
                assetResponse.contract.type
            ),
            links = assetResponse.links,
            attributes = assetResponse.attributes.map { NftAssetAttribute(it.trait_type, it.value, it.trait_count) }
        )
    }

    override suspend fun assetWithOrders(
        contractAddress: String,
        tokenId: String
    ): Pair<NftAssetRecord, List<AssetOrder>> {
        val assetResponse = HsNftModule.apiServiceV1.asset(contractAddress, tokenId)

        val orders = assetResponse.markets_data.orders?.let { orders ->
            orders.map { order ->
                val price = order.current_price.movePointLeft(order.payment_token_contract.decimals)
                AssetOrder(
                    closingDate = stringToDate(order.closing_date),
                    price = NftAssetPrice(getCoinTypeId(order.payment_token_contract.address), price),
                    emptyTaker = order.taker.address == zeroAddress,
                    side = order.side,
                    v = order.v,
                    ethValue = price.multiply(order.payment_token_contract.eth_price)
                )
            }
        } ?: listOf()

        val assetRecord = NftAssetRecord(
            accountId = "",
            collectionUid = assetResponse.collection_uid,
            tokenId = assetResponse.token_id,
            name = assetResponse.name,
            imageUrl = assetResponse.image_data?.image_url,
            imagePreviewUrl = assetResponse.image_data?.image_preview_url,
            description = assetResponse.description,
            onSale = assetResponse.markets_data.sell_orders?.isNotEmpty() ?: false,
            lastSale = assetResponse.markets_data.last_sale?.let { last_sale ->
                NftAssetPrice(
                    getCoinTypeId(last_sale.payment_token.address),
                    BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                )
            },
            contract = NftAssetContract(
                assetResponse.contract.address,
                assetResponse.contract.type
            ),
            links = assetResponse.links,
            attributes = assetResponse.attributes.map { NftAssetAttribute(it.trait_type, it.value, it.trait_count) }
        )

        return Pair(assetRecord, orders)
    }

    override suspend fun topCollections(count: Int): List<TopNftCollection> {
        return fetchTopCollections(count).map {
            TopNftCollection(
                uid = it.uid,
                name = it.name,
                imageUrl = it.image_data?.image_url,
                floorPrice = it.stats.floor_price,
                totalVolume = it.stats.total_volume,
                oneDayVolume = it.stats.one_day_volume,
                oneDayVolumeDiff = it.stats.one_day_change,
                sevenDayVolume = it.stats.seven_day_volume,
                sevenDayVolumeDiff = it.stats.seven_day_change,
                thirtyDayVolume = it.stats.thirty_day_volume,
                thirtyDayVolumeDiff = it.stats.thirty_day_change,
            )
        }
    }

    override suspend fun collection(uid: String): NftCollection {
        return HsNftModule.apiServiceV1.collection(uid, includeStatsChart = true).let {
            NftCollection(
                uid = it.uid,
                name = it.name,
                imageUrl = it.image_data?.image_url,
                description = it.description,
                ownersCount = it.stats.num_owners,
                totalSupply = it.stats.total_supply,
                oneDayVolume = it.stats.one_day_volume,
                oneDaySales = it.stats.one_day_sales,
                oneDayAveragePrice = it.stats.one_day_average_price,
                averagePrice = it.stats.average_price,
                floorPrice = it.stats.floor_price,
                chartPoints = it.stats_chart.map { chartPoint ->
                    NftCollection.ChartPoint(
                        timestamp = chartPoint.timestamp,
                        oneDayVolume = chartPoint.one_day_volume,
                        averagePrice = chartPoint.average_price,
                        floorPrice = chartPoint.floor_price,
                        oneDaySales = chartPoint.one_day_sales
                    )
                },
                links = NftCollection.Links(
                    externalUrl = it.links?.external_url,
                    discordUrl = it.links?.discord_url,
                    telegramUrl = it.links?.telegram_url,
                    twitterUsername = it.links?.twitter_username,
                    instagramUsername = it.links?.instagram_username,
                    wikiUrl = it.links?.wiki_url
                ),
                contracts = it.asset_contracts.map { contract ->
                    NftCollection.Contract(
                        address = contract.address,
                        type = contract.type
                    )
                },
                stats = NftCollection.CollectionStats(
                    averagePrice7d = NftAssetPrice(
                        getCoinTypeId(zeroAddress),
                        it.stats.seven_day_average_price
                    ),
                    averagePrice30d = NftAssetPrice(
                        getCoinTypeId(zeroAddress),
                        it.stats.thirty_day_average_price
                    ),
                    floorPrice = it.stats.floor_price?.let { floorPrice ->
                        NftAssetPrice(getCoinTypeId(zeroAddress), floorPrice)
                    }
                )
            )
        }
    }

    override suspend fun collectionAssets(
        uid: String,
        cursor: String?
    ): Pair<List<NftAssetRecord>, HsNftApiV1Response.Cursor> {
        val response = HsNftModule.apiServiceV1.assets(collectionUid = uid, cursor = cursor, limit = 50)
        val assetRecords = response.assets.map { assetResponse ->
            NftAssetRecord(
                accountId = "",
                collectionUid = assetResponse.collection_uid,
                tokenId = assetResponse.token_id,
                name = assetResponse.name,
                imageUrl = assetResponse.image_data?.image_url,
                imagePreviewUrl = assetResponse.image_data?.image_preview_url,
                description = assetResponse.description,
                onSale = assetResponse.markets_data.sell_orders?.isNotEmpty() ?: false,
                lastSale = assetResponse.markets_data.last_sale?.let { last_sale ->
                    NftAssetPrice(
                        getCoinTypeId(last_sale.payment_token.address),
                        BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                    )
                },
                contract = NftAssetContract(
                    assetResponse.contract.address,
                    assetResponse.contract.type
                ),
                links = assetResponse.links,
                attributes = assetResponse.attributes.map {
                    NftAssetAttribute(
                        it.trait_type,
                        it.value,
                        it.trait_count
                    )
                }
            )
        }
        return Pair(assetRecords, response.cursor)
    }

    override suspend fun collectionEvents(
        uid: String,
        type: String?,
        cursor: String?
    ): Pair<List<NftCollectionEvent>, HsNftApiV1Response.Cursor> {
        val response = HsNftModule.apiServiceV1.events(uid, type, cursor)

        val events = response.events.mapNotNull { event ->
            val assetResponse = event.asset

            val eventType = EventType.fromString(type) ?: EventType.Unknown
            try {
                val amount = event.markets_data.payment_token?.let {
                    NftAssetPrice(
                        getCoinTypeId(it.address),
                        BigDecimal(event.amount).movePointLeft(it.decimals)
                    )
                }

                NftCollectionEvent(
                    eventType = eventType,
                    asset = NftAssetRecord(
                        accountId = "",
                        collectionUid = assetResponse.collection_uid,
                        tokenId = assetResponse.token_id,
                        name = assetResponse.name,
                        imageUrl = assetResponse.image_data?.image_url,
                        imagePreviewUrl = assetResponse.image_data?.image_preview_url,
                        description = assetResponse.description,
                        onSale = assetResponse.markets_data.sell_orders?.isNotEmpty() ?: false,
                        lastSale = null,
                        contract = NftAssetContract(
                            assetResponse.contract.address,
                            assetResponse.contract.type
                        ),
                        links = assetResponse.links,
                        attributes = listOf()
                    ),
                    date = stringToDate(event.date),
                    amount = amount
                )
            } catch (e: Exception) {
                null
            }
        }

        return Pair(events, response.cursor)
    }

    private fun stringToDate(date: String) = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        sdf.parse(date)
    } catch (ex: Exception) {
        null
    }

    private suspend fun fetchTopCollections(count: Int): List<HsNftApiV1Response.Collection> {
        val collections = mutableListOf<HsNftApiV1Response.Collection>()
        var page = 1
        val limit = 300
        do {
            val collectionsResponse = HsNftModule.apiServiceV1.collections(page = page, limit = limit)
            collections.addAll(collectionsResponse)
            page++
        } while (collectionsResponse.size >= limit && collections.size < count)

        return collections
    }

    private suspend fun fetchAssets(address: Address): List<HsNftApiV1Response.Asset> {
        val assets = mutableListOf<HsNftApiV1Response.Asset>()
        var cursor: String? = null
        val limit = 50
        do {
            val response = HsNftModule.apiServiceV1.assets(owner = address.hex, cursor = cursor, limit = limit)
            assets.addAll(response.assets)
            cursor = response.cursor.next
        } while (response.assets.size >= limit)
        return assets
    }

    private suspend fun fetchCollections(address: Address): List<HsNftApiV1Response.Collection> {
        val collections = mutableListOf<HsNftApiV1Response.Collection>()
        var page = 1
        val limit = 300
        do {
            val collectionsResponse = HsNftModule.apiServiceV1.collections(address.hex, page, limit)
            collections.addAll(collectionsResponse)
            page++
        } while (collectionsResponse.size >= limit)

        return collections
    }

    private fun getCoinTypeId(paymentTokenAddress: String): String {
        val coinType = when (paymentTokenAddress) {
            zeroAddress -> CoinType.Ethereum
            else -> CoinType.Erc20(paymentTokenAddress.lowercase())
        }

        return coinType.id
    }
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
        @Query("collection_uid") collectionUid: String,
        @Query("event_type") eventType: String?,
        @Query("cursor") cursor: String? = null
    ): HsNftApiV1Response.Events

}
