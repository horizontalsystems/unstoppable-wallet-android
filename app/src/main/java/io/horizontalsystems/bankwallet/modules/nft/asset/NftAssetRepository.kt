package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Price
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem.Stats
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NftAssetRepository(
    private val xRateRepository: BalanceXRateRepository
) {
    private val _itemsFlow =
        MutableSharedFlow<NftAssetModuleAssetItem>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val itemsFlow = _itemsFlow.asSharedFlow()

    private val disposables = CompositeDisposable()

    fun start() {
        xRateRepository.itemObservable
            .subscribeIO { latestRates ->
                handleUpdatedRates(latestRates)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedRates(latestRates: Map<String, CoinPrice?>) {
        val currentItem = _itemsFlow.replayCache.firstOrNull() ?: return
        sync(currentItem, latestRates)
    }

    fun set(asset: NftAssetModuleAssetItem) {
        val stats = asset.stats
        val priceItems = listOf(
            stats.lastSale,
            stats.average7d,
            stats.average30d,
            stats.collectionFloor,
            stats.bestOffer,
            stats.sale?.price,
        )
        val coinUids = priceItems.mapNotNull { it?.coinValue?.coin?.uid }.distinct()
        xRateRepository.setCoinUids(coinUids)

        val latestRates = xRateRepository.getLatestRates()

        sync(asset, latestRates)
    }

    private fun sync(currentItem: NftAssetModuleAssetItem, latestRates: Map<String, CoinPrice?>) {
        val stats = currentItem.stats

        val updatedStats = Stats(
            lastSale = stats.lastSale?.setCurrencyValue(latestRates),
            average7d = stats.average7d?.setCurrencyValue(latestRates),
            average30d = stats.average30d?.setCurrencyValue(latestRates),
            collectionFloor = stats.collectionFloor?.setCurrencyValue(latestRates),
            bestOffer = stats.bestOffer?.setCurrencyValue(latestRates),
            sale = stats.sale?.copy(price = stats.sale.price?.setCurrencyValue(latestRates))
        )

        val updatedItem = currentItem.copy(stats = updatedStats)

        _itemsFlow.tryEmit(updatedItem)
    }

    private fun Price.setCurrencyValue(latestRates: Map<String, CoinPrice?>): Price {
        return copy(currencyValue = latestRates[coinValue.coin.uid]?.let { latestRate ->
            CurrencyValue(xRateRepository.baseCurrency, coinValue.value.multiply(latestRate.value))
        })
    }
}
