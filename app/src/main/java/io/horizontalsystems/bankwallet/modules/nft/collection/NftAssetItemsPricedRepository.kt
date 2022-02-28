package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetItemsPricedRepository {
    private val _itemsFlow = MutableSharedFlow<Map<NftCollectionRecord, List<NftAssetItemPriced>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val itemsFlow = _itemsFlow.asSharedFlow()

    var priceType = PriceType.Days7
        private set

    fun setPriceType(priceType: PriceType) {
        this.priceType = priceType

        _itemsFlow.replayCache.lastOrNull()?.let { collections ->
            val list = collections.map { (collectionItem, assetsPriced) ->
                val assets = assetsPriced.map { assetPriced ->
                    assetPriced.copy(coinPrice = getAssetPrice(assetPriced.assetItem, priceType))
                }
                collectionItem to assets
            }.toMap()

            _itemsFlow.tryEmit(list)
        }
    }

    fun setAssetItems(assetItems: Map<NftCollectionRecord, List<NftAssetItem>>) {
        val items = assetItems.map { (collectionRecord, assetItems) ->
            collectionRecord to assetItems.map { assetItem ->
                NftAssetItemPriced(
                    assetItem = assetItem,
                    coinPrice = getAssetPrice(assetItem, priceType)
                )
            }
        }.toMap()

        _itemsFlow.tryEmit(items)
    }

    private fun getAssetPrice(assetItem: NftAssetItem, priceType: PriceType) = when (priceType) {
        PriceType.Days7 -> assetItem.prices.average7d
        PriceType.Days30 -> assetItem.prices.average30d
        PriceType.LastSale -> assetItem.prices.last
    }
}
