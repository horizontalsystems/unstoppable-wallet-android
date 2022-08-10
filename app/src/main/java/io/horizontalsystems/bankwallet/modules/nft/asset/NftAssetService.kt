package io.horizontalsystems.bankwallet.modules.nft.asset

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.nft.CollectionLinks
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Price
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale.PriceType
import io.horizontalsystems.bankwallet.modules.nft.nftAssetPrice
import io.horizontalsystems.marketkit.models.AssetOrder
import io.horizontalsystems.marketkit.models.NftPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class NftAssetService(
    private val collectionUid: String,
    private val contractAddress: String,
    private val tokenId: String,
    private val marketKit: MarketKitWrapper,
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
            val collection = marketKit.nftCollection(collectionUid)
            val nftAsset = marketKit.nftAsset(contractAddress, tokenId)

            val (bestOffer, sale) = getOrderStats(nftAsset.orders)

            val asset = nftManager.assetItem(
                nftAsset,
                collectionName = collection.name,
                collectionLinks = CollectionLinks(
                    collection.externalUrl,
                    collection.discordUrl,
                    collection.twitterUsername
                ),
                totalSupply = collection.stats.totalSupply,
                averagePrice7d = collection.stats.averagePrice7d?.nftAssetPrice,
                averagePrice30d = collection.stats.averagePrice30d?.nftAssetPrice,
                floorPrice = collection.stats.floorPrice?.nftAssetPrice,
                bestOffer = bestOffer?.nftAssetPrice,
                sale = sale
            )

            repository.set(asset)
        } catch (error: Exception) {
            _serviceDataFlow.tryEmit(
                Result.failure(error)
            )
        }
    }

    private fun getOrderStats(orders: List<AssetOrder>): Pair<NftPrice?, Sale?> {
        var hasTopBid = false
        val sale: Sale?
        var bestOffer: NftPrice? = null

        val auctionOrders = orders.filter { it.side == 1 && it.v == null }.sortedBy { it.ethValue }
        val auctionOrder = auctionOrders.firstOrNull()

        if (auctionOrder != null) {
            val bidOrders = orders.filter { it.side == 0 && !it.emptyTaker }.sortedByDescending { it.ethValue }

            val type: PriceType
            val nftPrice: CoinValue?

            when (val bidOrder = bidOrders.firstOrNull()) {
                null -> {
                    type = PriceType.MinimumBid
                    nftPrice = auctionOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it.nftAssetPrice) }
                }
                else -> {
                    type = PriceType.TopBid
                    nftPrice = bidOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it.nftAssetPrice) }
                    hasTopBid = true
                }
            }

            sale = Sale(auctionOrder.closingDate, type, nftPrice?.let { Price(it) })
        } else {
            val buyNowOrders = orders.filter { it.side == 1 && it.v != null }.sortedBy { it.ethValue }

            sale = buyNowOrders.firstOrNull()?.let { buyNowOrder ->
                val price = buyNowOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it.nftAssetPrice) }?.let { Price(it) }
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
