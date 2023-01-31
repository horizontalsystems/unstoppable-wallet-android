package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.util.*

class NftEventsProvider(
    private val marketKit: MarketKitWrapper
) {
    private val apiURL = "https://api.reservoir.tools/"
    private val service: ReservoirApi = APIClient.retrofit(apiURL, 60).create(ReservoirApi::class.java)

    suspend fun collectionEventsMetadata(
        blockchainType: BlockchainType,
        providerUid: String,
        contractAddress: String,
        eventType: NftEventMetadata.EventType?,
        paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?> {
        val response = service.collectionActivity(contractAddress, eventTypeString(eventType), paginationData?.cursor)
        val eventsMetadata = events(blockchainType, response.activities, providerUid)
        return Pair(eventsMetadata, response.continuation?.let { PaginationData.Cursor(it) })
    }

    suspend fun assetEventsMetadata(
        nftUid: NftUid, eventType: NftEventMetadata.EventType?, paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?> {
        val response = service.tokenActivity(
            nftUid.contractAddress, nftUid.tokenId, eventTypeString(eventType), paginationData?.cursor
        )
        val eventsMetadata = events(nftUid.blockchainType, response.activities, null)

        return Pair(eventsMetadata, response.continuation?.let { PaginationData.Cursor(it) })
    }

    private fun events(blockchainType: BlockchainType, activities: List<Activity>, providerCollectionUid: String?): List<NftEventMetadata> {
        val token = marketKit.token(TokenQuery(blockchainType, TokenType.Native))

        return activities.map { activity ->
            val amount: NftPrice? = activity.price?.let { price -> token?.let { token -> NftPrice(token, price) } }

            NftEventMetadata(
                assetMetadata = NftAssetMetadata(
                    nftUid = NftUid.Evm(blockchainType, activity.collection?.collectionId ?: "", activity.token?.tokenId ?: ""),
                    providerCollectionUid = providerCollectionUid ?: activity.collection?.collectionId ?: "",
                    name = null,
                    imageUrl = activity.token?.tokenImage ?: activity.collection?.collectionImage,
                    previewImageUrl = activity.token?.tokenImage ?: activity.collection?.collectionImage,
                    description = null,
                    nftType = "",
                    externalLink = null,
                    providerLink = null,
                    traits = listOf(),
                    lastSalePrice = null,
                    offers = listOf(),
                    saleInfo = null,
                ),
                eventType = eventType(activity.type),
                date = activity.timestamp?.let { Date(it * 1000) },
                amount = amount,
            )
        }
    }


    private fun eventTypeString(eventType: NftEventMetadata.EventType?): String? = when (eventType) {
        NftEventMetadata.EventType.List -> "ask"
        NftEventMetadata.EventType.Sale -> "sale"
        NftEventMetadata.EventType.Transfer -> "transfer"
        NftEventMetadata.EventType.Mint -> "mint"
        NftEventMetadata.EventType.BidEntered -> "bid"
        NftEventMetadata.EventType.BidWithdrawn -> "bid_cancel"
        NftEventMetadata.EventType.Cancel -> "ask_cancel"
        else -> null
    }

    private fun eventType(openSeaEventType: String?): NftEventMetadata.EventType? = when (openSeaEventType) {
        "ask" -> NftEventMetadata.EventType.List
        "sale" -> NftEventMetadata.EventType.Sale
        "transfer" -> NftEventMetadata.EventType.Transfer
        "mint" -> NftEventMetadata.EventType.Mint
        "bid" -> NftEventMetadata.EventType.BidEntered
        "bid_cancel" -> NftEventMetadata.EventType.BidWithdrawn
        "ask_cancel" -> NftEventMetadata.EventType.Cancel
        else -> null
    }

    private interface ReservoirApi {
        @GET("tokens/{contractAddress}:{tokenId}/activity/v4")
        suspend fun tokenActivity(
            @Path("contractAddress") contractAddress: String,
            @Path("tokenId") tokenId: String,
            @Query("types") eventType: String?,
            @Query("continuation") cursor: String?
        ): ActivityResponse

        @GET("collections/activity/v5")
        suspend fun collectionActivity(
            @Query("collection") contractAddress: String, @Query("types") eventType: String?, @Query("continuation") cursor: String?
        ): ActivityResponse
    }

    data class ActivityResponse(val activities: List<Activity>, val continuation: String?)

    data class Activity(
        val type: String,
        val fromAddress: String?,
        val toAddress: String?,
        val price: BigDecimal?,
        val amount: Int?,
        val timestamp: Long?,
        val createdAt: Date?,
        val token: Token?,
        val collection: Collection?
    ) {
        data class Token(val tokenId: String?, val tokenName: String?, val tokenImage: String?)
        data class Collection(val collectionId: String?, val collectionName: String?, val collectionImage: String?)
    }
}
