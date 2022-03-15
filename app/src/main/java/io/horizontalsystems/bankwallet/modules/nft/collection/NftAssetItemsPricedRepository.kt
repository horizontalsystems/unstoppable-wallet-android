package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetItemsPricedRepository {
    private val _itemsDataFlow = MutableSharedFlow<DataWithError<Map<NftCollectionRecord, List<NftAssetItemPriced>>?>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val itemsDataFlow = _itemsDataFlow.asSharedFlow()

    var priceType = PriceType.Days7
        private set

    fun setPriceType(priceType: PriceType) {
        this.priceType = priceType

        _itemsDataFlow.replayCache.lastOrNull()?.let { data ->
            val list = data.value?.map { (collectionItem, assetsPriced) ->
                val assets = assetsPriced.map { assetPriced ->
                    assetPriced.copy(coinPrice = getAssetPrice(assetPriced.assetItem, priceType))
                }
                collectionItem to assets
            }?.toMap()

            _itemsDataFlow.tryEmit(DataWithError(list, null))
        }
    }

    fun setAssetItems(data: DataWithError<Map<NftCollectionRecord, List<NftAssetItem>>?>) {
        val items = data.value?.map { (collectionRecord, assetItems) ->
            collectionRecord to assetItems.map { assetItem ->
                NftAssetItemPriced(
                    assetItem = assetItem,
                    coinPrice = getAssetPrice(assetItem, priceType)
                )
            }
        }?.toMap()

        _itemsDataFlow.tryEmit(DataWithError(items, data.error))
    }

    private fun getAssetPrice(assetItem: NftAssetItem, priceType: PriceType) = when (priceType) {
        PriceType.Days7 -> assetItem.prices.average7d
        PriceType.Days30 -> assetItem.prices.average30d
        PriceType.LastSale -> assetItem.prices.last
    }
}
