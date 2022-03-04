package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.nft.NftAssetAttribute
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.*
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Sale.PriceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class NftAssetService(
    private val accountId: String,
    private val tokenId: String,
    private val contractAddress: String,
    private val nftManager: NftManager,
    private val repository: NftAssetRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val _serviceItemState = MutableStateFlow<DataState<NftAssetModuleAssetItem>>(DataState.Loading)

    val serviceItemState = _serviceItemState.asStateFlow()

    fun start() {
        coroutineScope.launch {
            repository.itemsFlow.collect { asset ->
                _serviceItemState.value = DataState.Success(asset)
            }
        }

        coroutineScope.launch {
            val assetRecord =
                nftManager.getAssetRecord(accountId, tokenId, contractAddress) ?: throw NftNotFoundException()
            val collectionRecord =
                nftManager.getCollectionRecord(accountId, assetRecord.collectionUid) ?: throw NftNotFoundException()

            val asset = NftAssetModuleAssetItem(
                name = assetRecord.name,
                imageUrl = assetRecord.imageUrl,
                collectionName = collectionRecord.name,
                ownedCount = assetRecord.ownedCount,
                description = assetRecord.description,
                contract = assetRecord.contract,
                tokenId = assetRecord.tokenId,
                assetLinks = assetRecord.links,
                collectionLinks = collectionRecord.links,
                stats = Stats(
                    lastSale = nftManager.nftAssetPriceToCoinValue(assetRecord.lastSale)?.let { Price(it) },
                    average7d = nftManager.nftAssetPriceToCoinValue(collectionRecord.averagePrice7d)
                        ?.let { Price(it) },
                    average30d = nftManager.nftAssetPriceToCoinValue(collectionRecord.averagePrice30d)
                        ?.let { Price(it) },
                ),
                attributes = assetRecord.attributes.map { attribute ->
                    Attribute(
                        attribute.type,
                        attribute.value,
                        getAttributePercentage(attribute, collectionRecord.totalSupply)?.let { "$it%" })
                }
            )

            repository.set(asset)

            syncStats(asset, tokenId, contractAddress, assetRecord.collectionUid)
        }
    }

    private fun getAttributePercentage(attribute: NftAssetAttribute, totalSupply: Int): Number? =
        if (attribute.count > 0 && totalSupply > 0) {
            val percent = (attribute.count * 100f / totalSupply)
            when {
                percent >= 10 -> percent.roundToInt()
                percent >= 1 -> (percent * 10).roundToInt() / 10f
                else -> (percent * 100).roundToInt() / 100f
            }
        } else {
            null
        }

    private suspend fun syncStats(
        asset: NftAssetModuleAssetItem,
        tokenId: String,
        contractAddress: String,
        collectionUid: String
    ) {
        val assetOrders = nftManager.assetOrders(contractAddress, tokenId)
        val collectionStats = nftManager.collectionStats(collectionUid)

        val (bestOffer, sale) = getOrderStats(assetOrders)
        val newStatsItem = Stats(
            lastSale = asset.stats.lastSale,
            average7d = nftManager.nftAssetPriceToCoinValue(collectionStats.averagePrice7d)?.let { Price(it) },
            average30d = nftManager.nftAssetPriceToCoinValue(collectionStats.averagePrice30d)?.let { Price(it) },
            collectionFloor = nftManager.nftAssetPriceToCoinValue(collectionStats.floorPrice)?.let { Price(it) },
            bestOffer = bestOffer,
            sale = sale
        )

        repository.set(asset.copy(stats = newStatsItem))
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

class NftNotFoundException : Exception()