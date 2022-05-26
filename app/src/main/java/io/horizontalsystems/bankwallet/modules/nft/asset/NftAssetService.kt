package io.horizontalsystems.bankwallet.modules.nft.asset

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiV1Response
import io.horizontalsystems.bankwallet.modules.nft.INftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.NftAssetPrice
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Price
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale.PriceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class NftAssetService(
    private val collectionUid: String,
    private val contractAddress: String,
    private val tokenId: String,
    private val nftApiProvider: INftApiProvider,
    private val nftManager: NftManager,
    private val repository: NftAssetRepository
) {
    private val _serviceDataFlow =
        MutableSharedFlow<Result<NftAssetModuleAssetItem>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val serviceDataFlow = _serviceDataFlow.asSharedFlow()

    suspend fun start() = withContext(Dispatchers.IO) {
        repository.dataFlow.collectWith(this) { assetData ->
            _serviceDataFlow.tryEmit(Result.success(assetData))
        }

        loadAsset()
        repository.start()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        loadAsset()
    }

    private suspend fun loadAsset() {
        try {
            val collection = nftApiProvider.collection(collectionUid)
            val (assetRecord, assetOrders) = nftApiProvider.assetWithOrders(contractAddress, tokenId)

            val (bestOffer, sale) = getOrderStats(assetOrders)

            val asset = nftManager.assetItem(
                assetRecord = assetRecord,
                collectionName = collection.name,
                collectionLinks = collection.links?.let {
                    HsNftApiV1Response.Collection.Links(
                        it.externalUrl,
                        it.discordUrl,
                        it.telegramUrl,
                        it.twitterUsername,
                        it.instagramUsername,
                        it.wikiUrl
                    )
                },
                totalSupply = collection.totalSupply,
                averagePrice7d = collection.stats.averagePrice7d,
                averagePrice30d = collection.stats.averagePrice30d,
                floorPrice = collection.stats.floorPrice,
                bestOffer = bestOffer,
                sale = sale
            )

            repository.set(asset)
        } catch (error: Exception) {
            _serviceDataFlow.tryEmit(
                Result.failure(error)
            )
        }
    }

    private fun getOrderStats(orders: List<AssetOrder>): Pair<NftAssetPrice?, Sale?> {
        var hasTopBid = false
        val sale: Sale?
        var bestOffer: NftAssetPrice? = null

        val auctionOrders = orders.filter { it.side == 1 && it.v == null }.sortedBy { it.ethValue }
        val auctionOrder = auctionOrders.firstOrNull()

        if (auctionOrder != null) {
            val bidOrders = orders.filter { it.side == 0 && !it.emptyTaker }.sortedByDescending { it.ethValue }

            val type: PriceType
            val nftPrice: CoinValue?

            when (val bidOrder = bidOrders.firstOrNull()) {
                null -> {
                    type = PriceType.MinimumBid
                    nftPrice = auctionOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it) }
                }
                else -> {
                    type = PriceType.TopBid
                    nftPrice = bidOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it) }
                    hasTopBid = true
                }
            }

            sale = Sale(auctionOrder.closingDate, type, nftPrice?.let { Price(it) })
        } else {
            val buyNowOrders = orders.filter { it.side == 1 && it.v != null }.sortedBy { it.ethValue }

            sale = buyNowOrders.firstOrNull()?.let { buyNowOrder ->
                val price = buyNowOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it) }?.let { Price(it) }
                Sale(buyNowOrder.closingDate, PriceType.BuyNow, price)
            }
        }

        if (!hasTopBid) {
            val offerOrders = orders.filter { it.side == 0 }.sortedByDescending { it.ethValue }

            bestOffer = offerOrders.firstOrNull()?.price
        }

        return Pair(bestOffer, sale)
    }

    fun stop() {
        repository.stop()
    }
}
