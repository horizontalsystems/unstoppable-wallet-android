package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.*
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale.PriceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NftAssetService(
    private val asset: NftAssetModuleAssetItem,
    private val nftManager: NftManager,
    private val repository: NftAssetRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _serviceDataFlow = MutableSharedFlow<DataWithError<NftAssetModuleAssetItem>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val serviceDataFlow = _serviceDataFlow.asSharedFlow()

    fun start() {
        coroutineScope.launch {
            repository.dataFlow.collect { assetData ->
                _serviceDataFlow.tryEmit(assetData)
            }
        }

        coroutineScope.launch {
            loadAsset()
        }
    }

    suspend fun refresh() {
        loadAsset()
    }

    private suspend fun loadAsset() {
        repository.set(DataWithError(asset, null))

        syncStats(asset)
    }

    private suspend fun syncStats(asset: NftAssetModuleAssetItem) {
        try {
            val assetOrders = nftManager.assetOrders(asset.contract.address, asset.tokenId)
            val collectionStats = nftManager.collectionStats(asset.collectionUid)

            val (bestOffer, sale) = getOrderStats(assetOrders)
            val newStatsItem = Stats(
                lastSale = asset.stats.lastSale,
                average7d = nftManager.nftAssetPriceToCoinValue(collectionStats.averagePrice7d)?.let { Price(it) },
                average30d = nftManager.nftAssetPriceToCoinValue(collectionStats.averagePrice30d)?.let { Price(it) },
                collectionFloor = nftManager.nftAssetPriceToCoinValue(collectionStats.floorPrice)?.let { Price(it) },
                bestOffer = bestOffer,
                sale = sale
            )

            repository.set(DataWithError(asset.copy(stats = newStatsItem), null))
        } catch (error: Exception) {
            repository.set(DataWithError(asset, error))
        }
    }

    private fun getOrderStats(orders: List<AssetOrder>): Pair<Price?, Sale?> {
        var hasTopBid = false
        val sale: Sale?
        var bestOffer: Price? = null

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

            bestOffer = offerOrders.firstOrNull()?.let { offerOrder ->
                offerOrder.price?.let { nftManager.nftAssetPriceToCoinValue(it) }?.let { Price(it) }
            }
        }

        return Pair(bestOffer, sale)
    }
}
