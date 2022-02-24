package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NftAssetItemsPricedWithCurrencyRepository(
    private val xRateRepository: BalanceXRateRepository
) {
    private val _itemsFlow = MutableStateFlow<Map<NftCollectionRecord, List<NftAssetItemPricedWithCurrency>>>(mapOf())
    val itemsFlow = _itemsFlow.asStateFlow()

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
        _itemsFlow.update {
            it.map { (collectionRecord, items) ->
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
            }.toMap()
        }
    }

    fun setItems(itemsPriced: Map<NftCollectionRecord, List<NftAssetItemPriced>>) {
        val coinUids = itemsPriced.map { (_, items) ->
            items.mapNotNull {
                it.coinPrice?.coin?.uid
            }
        }.flatten()

        xRateRepository.setCoinUids(coinUids)
        val latestRates = xRateRepository.getLatestRates()


        val items = itemsPriced.map { (collectionRecord, assetItemsPriced) ->
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

        _itemsFlow.update { items }
    }

    fun stop() {
        disposables.clear()
    }

    fun refresh() {
        xRateRepository.refresh()
    }
}
