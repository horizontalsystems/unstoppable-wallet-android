package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetShortMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionShortMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftContractMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata.EventType
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.NftPrice
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class OpenSeaNftProvider(
    private val marketKit: MarketKitWrapper,
    appConfigProvider: AppConfigProvider
) : INftProvider {

    private val service: OpenSeaService = OpenSeaService(appConfigProvider.marketApiBaseUrl, appConfigProvider.marketApiKey)
    private val zeroAddress = "0x0000000000000000000000000000000000000000"

    override val title = "OpenSea"
    override val icon = R.drawable.ic_opensea_20

    override suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata {
        val collectionsResponse = service.allCollections(address)
        val collections = collections(blockchainType, collectionsResponse)

        val assetsResponse = service.allAssets(address)
        val assets = assets(blockchainType, assetsResponse)

        return NftAddressMetadata(
            collections = collections.map {
                NftCollectionShortMetadata(
                    providerUid = it.providerUid,
                    name = it.name,
                    thumbnailImageUrl = it.imageUrl ?: it.thumbnailImageUrl,
                    averagePrice7d = it.stats7d?.averagePrice,
                    averagePrice30 = it.stats30d?.averagePrice
                )
            },
            assets = assets.map {
                NftAssetShortMetadata(
                    nftUid = it.nftUid,
                    providerCollectionUid = it.providerCollectionUid,
                    name = it.name,
                    previewImageUrl = it.previewImageUrl,
                    onSale = it.saleInfo != null,
                    lastSalePrice = it.lastSalePrice
                )
            }
        )
    }

    override suspend fun extendedAssetMetadata(nftUid: NftUid, providerCollectionUid: String): Pair<NftAssetMetadata, NftCollectionMetadata> {
        val asset = service.asset(nftUid.contractAddress, nftUid.tokenId)
        val collection = service.collection(providerCollectionUid)

        return Pair(assetMetadata(nftUid.blockchainType, asset), collectionMetadata(nftUid.blockchainType, collection))
    }

    override suspend fun collectionMetadata(blockchainType: BlockchainType, providerUid: String): NftCollectionMetadata {
        val response = service.collection(providerUid)
        return collectionMetadata(blockchainType, response)
    }

    override suspend fun collectionAssetsMetadata(
        blockchainType: BlockchainType,
        providerUid: String,
        paginationData: PaginationData?
    ): Pair<List<NftAssetMetadata>, PaginationData?> {
        val response = service.collectionAssets(providerUid, paginationData?.cursor)
        val assetsMetadata = assets(blockchainType, response.assets)
        return Pair(assetsMetadata, response.next?.let { PaginationData.Cursor(it) })
    }

    override suspend fun collectionEventsMetadata(
        blockchainType: BlockchainType,
        providerUid: String,
        eventType: EventType?,
        paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?> {
        val response = service.collectionEvents(providerUid, openSeaEventType(eventType), paginationData?.cursor)
        val eventsMetadata = events(blockchainType, response.asset_events)
        return Pair(eventsMetadata, response.next?.let { PaginationData.Cursor(it) })
    }

    override suspend fun assetEventsMetadata(
        nftUid: NftUid,
        eventType: EventType?,
        paginationData: PaginationData?
    ): Pair<List<NftEventMetadata>, PaginationData?> {
        val response = service.assetEvents(nftUid.contractAddress, nftUid.tokenId, openSeaEventType(eventType), paginationData?.cursor)
        val eventsMetadata = events(nftUid.blockchainType, response.asset_events)
        return Pair(eventsMetadata, response.next?.let { PaginationData.Cursor(it) })
    }

    override suspend fun assetsBriefMetadata(blockchainType: BlockchainType, nftUids: List<NftUid>): List<NftAssetBriefMetadata> {
        val chunkedNftUids = nftUids.chunked(30)
        val assetsBriefList = mutableListOf<NftAssetBriefMetadata>()
        for (nftUidsChunk in chunkedNftUids) {
            try {
                val response = service.assets(contractAddresses = nftUidsChunk.map { it.contractAddress }, tokenIds = nftUidsChunk.map { it.tokenId })
                assetsBriefList.addAll(assetsBrief(blockchainType, response.assets))
            } catch (error: Throwable) {
                continue
            }
        }
        return assetsBriefList
    }

    private fun nftPrice(token: Token?, value: BigDecimal?, shift: Boolean): NftPrice? {
        token ?: return null
        value ?: return null

        return NftPrice(
            token,
            if (shift) value.movePointLeft(token.decimals) else value
        )
    }

    private fun stringToDate(date: String) = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
        sdf.parse(date)
    } catch (ex: Exception) {
        null
    }

    private fun collectionMetadata(
        blockchainType: BlockchainType,
        response: OpenSeaNftApiResponse.Collection,
        baseToken: Token? = null
    ): NftCollectionMetadata {
        val baseToken = baseToken ?: marketKit.token(TokenQuery(blockchainType, TokenType.Native))

        return NftCollectionMetadata(
            blockchainType = blockchainType,
            providerUid = response.slug,
            contracts = response.primary_asset_contracts?.map { NftContractMetadata(it.address, it.name, it.created_date, it.schema_name) } ?: listOf(),
            name = response.name,
            description = response.description,
            imageUrl = response.large_image_url,
            thumbnailImageUrl = response.image_url,
            externalUrl = response.external_url,
            providerUrl = "https://opensea.io/collection/${response.slug}",
            discordUrl = response.discord_url,
            twitterUsername = response.twitter_username,
            count = response.stats?.count,
            ownerCount = response.stats?.num_owners,
            totalSupply = response.stats?.total_supply,
            totalVolume = response.stats?.total_volume,
            floorPrice = nftPrice(baseToken, response.stats?.floor_price, false),
            marketCap = nftPrice(baseToken, response.stats?.market_cap, false),
            royalty = response.dev_seller_fee_basis_points?.divide(BigDecimal(100)),
            inceptionDate = response.primary_asset_contracts?.firstOrNull()?.created_date?.let { stringToDate(it) },
            stats1d = NftCollectionMetadata.Stats(
                volume = nftPrice(baseToken, response.stats?.one_day_volume, false),
                change = response.stats?.one_day_change,
                sales = response.stats?.one_day_sales,
                averagePrice = nftPrice(baseToken, response.stats?.one_day_average_price, false),
            ),
            stats7d = NftCollectionMetadata.Stats(
                volume = nftPrice(baseToken, response.stats?.seven_day_volume, false),
                change = response.stats?.seven_day_change,
                sales = response.stats?.seven_day_sales,
                averagePrice = nftPrice(baseToken, response.stats?.seven_day_average_price, false),
            ),
            stats30d = NftCollectionMetadata.Stats(
                volume = nftPrice(baseToken, response.stats?.thirty_day_volume, false),
                change = response.stats?.thirty_day_change,
                sales = response.stats?.thirty_day_sales,
                averagePrice = nftPrice(baseToken, response.stats?.thirty_day_average_price, false),
            )
        )
    }

    private fun tokenType(address: String): TokenType =
        if (address == zeroAddress) TokenType.Native else TokenType.Eip20(address)

    private fun tokenMap(blockchainType: BlockchainType, addresses: List<String>): Map<String, Token> =
        try {
            val map = mutableMapOf<String, Token>()
            val tokenTypes = addresses.map { tokenType(it) }
            val tokens = marketKit.tokens(tokenTypes.map { TokenQuery(blockchainType, it) })
            tokens.forEach { token ->
                when (val tokenType = token.type) {
                    TokenType.Native -> map[zeroAddress] = token
                    is TokenType.Eip20 -> map[tokenType.address] = token
                    else -> Unit
                }
            }
            map
        } catch (error: Throwable) {
            emptyMap()
        }

    private fun assetMetadata(
        blockchainType: BlockchainType,
        response: OpenSeaNftApiResponse.Asset,
        tokenMap: Map<String, Token>? = null
    ): NftAssetMetadata {
        val map = if (tokenMap != null) tokenMap else {
            val addresses = mutableListOf<String>()

            response.last_sale?.let {
                addresses.add(it.payment_token.address.lowercase())
            }
            response.orders.forEach { order ->
                (order.offer + order.consideration).forEach { offer ->
                    addresses.add(offer.token.lowercase())
                }
            }
            tokenMap(blockchainType, addresses.distinct())
        }

        val bidOrders = response.orders.filter { it.side == "bid" && it.order_type == "criteria" }
        val offers = bidOrders.mapNotNull { order ->
            order.offer.firstOrNull()?.let { offer ->
                map[offer.token.lowercase()]?.let { token ->
                    nftPrice(token, order.current_price, true)
                }
            }
        }

        val basicAskOrders = response.orders.filter { it.side == "ask" && it.order_type == "basic" }
        val englishAskOrders = response.orders.filter { it.side == "ask" && it.order_type == "english" }

        val saleInfo: NftAssetMetadata.SaleInfo? = if (basicAskOrders.isNotEmpty()) {
            saleInfo(NftAssetMetadata.SaleType.OnSale, basicAskOrders, map)
        } else if (englishAskOrders.isNotEmpty()) {
            saleInfo(NftAssetMetadata.SaleType.OnAuction, englishAskOrders, map)
        } else {
            null
        }

        return NftAssetMetadata(
            nftUid = NftUid.Evm(blockchainType, response.asset_contract.address, response.token_id),
            providerCollectionUid = response.collection.slug,
            name = response.name,
            imageUrl = response.image_url,
            previewImageUrl = response.image_preview_url,
            description = response.description,
            nftType = response.asset_contract.schema_name,
            externalLink = response.external_link,
            providerLink = response.permalink,
            traits = response.traits?.map {
                NftAssetMetadata.Trait(
                    it.trait_type,
                    it.value,
                    it.trait_count,
                    traitSearchUrl(it.trait_type, it.value, response.collection.slug)
                )
            } ?: listOf(),
            lastSalePrice = response.last_sale?.let { nftPrice(map[it.payment_token.address.lowercase()], it.total_price, true) },
            offers = offers,
            saleInfo = saleInfo
        )
    }

    private fun saleInfo(
        type: NftAssetMetadata.SaleType,
        orders: List<OpenSeaNftApiResponse.Asset.Order>,
        map: Map<String, Token>
    ) = NftAssetMetadata.SaleInfo(
        type,
        listings = orders.mapNotNull { order ->
            order.consideration.firstOrNull()?.let { consideration ->
                map[consideration.token.lowercase()]?.let { token ->
                    val price = NftPrice(token, order.current_price.movePointLeft(token.decimals))
                    NftAssetMetadata.SaleListing(Date(order.expiration_time), price = price)
                }
            }
        }
    )

    private fun assets(blockchainType: BlockchainType, responses: List<OpenSeaNftApiResponse.Asset>): List<NftAssetMetadata> {
        val addresses = mutableListOf<String>()
        responses.forEach { response ->
            response.last_sale?.let {
                addresses.add(it.payment_token.address.lowercase())
            }
            response.orders.forEach { order ->
                (order.offer + order.consideration).forEach { offer ->
                    addresses.add(offer.token.lowercase())
                }
            }
        }
        val tokenMap = tokenMap(blockchainType, addresses.distinct())
        return responses.map { assetMetadata(blockchainType, it, tokenMap) }
    }

    private fun collections(blockchainType: BlockchainType, responses: List<OpenSeaNftApiResponse.Collection>): List<NftCollectionMetadata> {
        val baseToken = marketKit.token(TokenQuery(blockchainType, TokenType.Native))
        return responses.map { collectionMetadata(blockchainType, it, baseToken) }
    }

    private fun events(blockchainType: BlockchainType, responses: List<OpenSeaNftApiResponse.Event>): List<NftEventMetadata> {
        val addresses = mutableListOf<String>()
        responses.forEach { response ->
            response.payment_token?.address?.let {
                addresses.add(it.lowercase())
            }
        }
        val tokenMap = tokenMap(blockchainType, addresses.distinct())

        return responses.mapNotNull { response ->
            response.asset?.let { asset ->
                val amount: NftPrice? = response.payment_token?.let { paymentToken ->
                    response.total_price?.let { value ->
                        nftPrice(tokenMap[paymentToken.address], value, true)
                    }
                }
                NftEventMetadata(
                    assetMetadata = assetMetadata(blockchainType, asset, mapOf()),
                    eventType = eventType(response.event_type),
                    date = stringToDate(response.event_timestamp),
                    amount = amount
                )
            }
        }
    }

    private fun assetsBrief(blockchainType: BlockchainType, assets: List<OpenSeaNftApiResponse.Asset>): List<NftAssetBriefMetadata> = assets.map {
        NftAssetBriefMetadata(
            nftUid = NftUid.Evm(blockchainType, it.asset_contract.address, it.token_id),
            providerCollectionUid = it.collection.slug,
            name = it.name,
            it.image_url,
            it.image_preview_url
        )
    }

    private fun openSeaEventType(eventType: EventType?): String? =
        when (eventType) {
            EventType.List -> "created"
            EventType.Sale -> "successful"
            EventType.OfferEntered -> "offer_entered"
            EventType.BidEntered -> "bid_entered"
            EventType.BidWithdrawn -> "bid_withdrawn"
            EventType.Transfer -> "transfer"
            EventType.Approve -> "approve"
            EventType.Custom -> "custom"
            EventType.Payout -> "payout"
            EventType.Cancel -> "cancelled"
            EventType.BulkCancel -> "bulk_cancel"
            EventType.All,
            EventType.Unknown,
            EventType.Mint,
            null -> null
        }

    private fun eventType(openSeaEventType: String?): EventType? =
        when (openSeaEventType) {
            "created" -> EventType.List
            "successful" -> EventType.Sale
            "offer_entered" -> EventType.OfferEntered
            "bid_entered" -> EventType.BidEntered
            "bid_withdrawn" -> EventType.BidWithdrawn
            "transfer" -> EventType.Transfer
            "approve" -> EventType.Approve
            "custom" -> EventType.Custom
            "payout" -> EventType.Payout
            "cancelled" -> EventType.Cancel
            "bulk_cancel" -> EventType.BulkCancel
            else -> null
        }

    private fun traitSearchUrl(type: String, value: String, collectionUid: String): String {
        return "https://opensea.io/assets/${collectionUid}?search[stringTraits][0][name]=${type}" +
                "&search[stringTraits][0][values][0]=${value}" +
                "&search[sortAscending]=true&search[sortBy]=PRICE"
    }
}