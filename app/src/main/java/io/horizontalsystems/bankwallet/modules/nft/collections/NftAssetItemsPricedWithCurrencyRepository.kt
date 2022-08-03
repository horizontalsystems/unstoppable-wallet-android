package io.horizontalsystems.bankwallet.modules.nft.collections

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.bankwallet.modules.nft.collection.assets.CollectionAsset
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetItemsPricedWithCurrencyRepository(
    private val xRateRepository: BalanceXRateRepository
) {
    private val _itemsDataFlow =
        MutableSharedFlow<DataWithError<Map<NftCollectionRecord, List<CollectionAsset>>?>>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val itemsDataFlow = _itemsDataFlow.asSharedFlow()

    val baseCurrency by xRateRepository::baseCurrency

    private val disposables = CompositeDisposable()

    fun start() {
        xRateRepository.itemObservable
            .subscribeIO {
                handleUpdatedRates(it)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedRates(latestRates: Map<String, CoinPrice?>) {
        val currentData = _itemsDataFlow.replayCache.lastOrNull() ?: return
        val updatedValue = currentData.value?.map { (collectionRecord, items) ->
            collectionRecord to updateRates(items, latestRates)
        }?.toMap()

        _itemsDataFlow.tryEmit(currentData.copy(value = updatedValue))
    }

    fun setItems(data: DataWithError<Map<NftCollectionRecord, List<CollectionAsset>>?>) {
        val items = data.value?.let { value ->
            val coinUids = value.map { (_, items) ->
                items.mapNotNull { it.price?.coinValue?.coin?.uid }
            }.flatten().distinct()

            xRateRepository.setCoinUids(coinUids)
            val latestRates = xRateRepository.getLatestRates()

            value.map { (collectionRecord, assets) ->
                collectionRecord to updateRates(assets, latestRates)
            }.toMap()
        }

        _itemsDataFlow.tryEmit(DataWithError(items, data.error))
    }

    private fun updateRates(
        assets: List<CollectionAsset>,
        latestRates: Map<String, CoinPrice?>
    ) = assets.map { asset ->
        val coinPrice = asset.price?.coinValue
        coinPrice?.let {
            latestRates[coinPrice.coin.uid]?.let { latestRate ->
                val currencyPrice = CurrencyValue(xRateRepository.baseCurrency, coinPrice.value.multiply(latestRate.value))
                asset.copy(price = NftAssetModuleAssetItem.Price(coinPrice, currencyPrice), xRate = latestRate)
            }
        } ?: asset
    }

    fun stop() {
        disposables.clear()
    }

    fun refresh() {
        xRateRepository.refresh()
    }
}
