package io.horizontalsystems.bankwallet.modules.nft.collections

import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.bankwallet.modules.nft.collection.assets.CollectionAsset
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetItemsPricedRepository {
    private val _itemsDataFlow = MutableSharedFlow<DataWithError<Map<NftCollectionRecord, List<CollectionAsset>>?>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val itemsDataFlow = _itemsDataFlow.asSharedFlow()

    var priceType = PriceType.LastSale
        private set

    fun setPriceType(priceType: PriceType) {
        this.priceType = priceType

        _itemsDataFlow.replayCache.lastOrNull()?.let { data ->
            val list = data.value?.map { (collectionItem, assets) ->
                collectionItem to assets.map { asset ->
                    asset.copy(price = getAssetPrice(asset.asset, priceType))
                }
            }?.toMap()

            _itemsDataFlow.tryEmit(DataWithError(list, null))
        }
    }

    fun setAssetItems(data: DataWithError<Map<NftCollectionRecord, List<NftAssetModuleAssetItem>>?>) {
        val items = data.value?.map { (collectionRecord, assetItems) ->
            collectionRecord to assetItems.map { assetItem ->
                CollectionAsset(
                    asset = assetItem,
                    price = getAssetPrice(assetItem, priceType)
                )
            }
        }?.toMap()

        _itemsDataFlow.tryEmit(DataWithError(items, data.error))
    }

    private fun getAssetPrice(
        assetItem: NftAssetModuleAssetItem,
        priceType: PriceType
    ): NftAssetModuleAssetItem.Price? = when (priceType) {
        PriceType.Days7 -> assetItem.stats.average7d
        PriceType.Days30 -> assetItem.stats.average30d
        PriceType.LastSale -> assetItem.stats.lastSale
    }
}
