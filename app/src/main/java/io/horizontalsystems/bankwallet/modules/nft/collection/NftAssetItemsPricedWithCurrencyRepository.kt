package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetItemsPricedWithCurrencyRepository(
    private val xRateRepository: BalanceXRateRepository
) {
    private val _itemsDataFlow =
        MutableSharedFlow<DataWithError<Map<NftCollectionRecord, List<NftAssetItemPricedWithCurrency>>?>>(
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
            collectionRecord to items.map { item ->
                val coinPrice = item.coinPrice
                val itemCoinUid = coinPrice?.coin?.uid
                if (latestRates.containsKey(itemCoinUid)) {
                    val currencyPrice = latestRates[itemCoinUid]?.let { latestRate ->
                        coinPrice?.let {
                            CurrencyValue(xRateRepository.baseCurrency, coinPrice.value.multiply(latestRate.value))
                        }
                    }
                    item.copy(currencyPrice = currencyPrice)
                } else {
                    item
                }
            }
        }?.toMap()

        _itemsDataFlow.tryEmit(currentData.copy(value = updatedValue))
    }

    fun setItems(data: DataWithError<Map<NftCollectionRecord, List<NftAssetItemPriced>>?>) {
        val items = data.value?.let { value ->
            val coinUids = value.map { (_, items) ->
                items.mapNotNull {
                    it.coinPrice?.coin?.uid
                }
            }.flatten()

            xRateRepository.setCoinUids(coinUids)
            val latestRates = xRateRepository.getLatestRates()

            value.map { (collectionRecord, assetItemsPriced) ->
                collectionRecord to assetItemsPriced.map {
                    val currencyPrice = it.coinPrice?.let { coinPrice ->
                        latestRates[coinPrice.coin.uid]?.let { latestRate ->
                            CurrencyValue(xRateRepository.baseCurrency, coinPrice.value.multiply(latestRate.value))
                        }
                    }

                    NftAssetItemPricedWithCurrency(
                        assetItem = it.assetItem,
                        coinPrice = it.coinPrice,
                        currencyPrice = currencyPrice
                    )
                }
            }.toMap()
        }

        _itemsDataFlow.tryEmit(DataWithError(items, data.error))
    }

    fun stop() {
        disposables.clear()
    }

    fun refresh() {
        xRateRepository.refresh()
    }
}
