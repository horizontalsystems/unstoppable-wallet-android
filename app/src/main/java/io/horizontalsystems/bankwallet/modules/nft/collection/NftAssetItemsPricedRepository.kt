package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NftAssetItemsPricedRepository {
    private val _itemsFlow = MutableStateFlow<Map<NftCollectionRecord, List<NftAssetItemPriced>>>(mapOf())
    val itemsFlow = _itemsFlow.asStateFlow()

    var priceType = PriceType.Days7
        private set

    fun setPriceType(priceType: PriceType) {
        this.priceType = priceType

        _itemsFlow.value.let { collections ->
            val list = collections.map { (collectionItem, assetsPriced) ->
                val assets = assetsPriced.map { assetPriced ->
                    assetPriced.copy(coinPrice = getAssetPrice(assetPriced.assetItem, priceType))
                }
                collectionItem to assets
            }.toMap()

            _itemsFlow.update { list }
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

        _itemsFlow.update { items }
    }

    private fun getAssetPrice(assetItem: NftAssetItem, priceType: PriceType) = when (priceType) {
        PriceType.Days7 -> assetItem.prices.average7d
        PriceType.Days30 -> assetItem.prices.average30d
        PriceType.LastPrice -> assetItem.prices.last
    }
}
