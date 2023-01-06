package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsModule.VolumeMenuType
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketTicker
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class CoinMarketsService(
    val fullCoin: FullCoin,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) {

    private val disposables = CompositeDisposable()
    private var marketTickers = listOf<MarketTicker>()
    private val price = marketKit.coinPrice(fullCoin.coin.uid, currencyManager.baseCurrency.code)?.value ?: BigDecimal.ZERO

    private val volumeOptions = listOf(
        VolumeMenuType.Coin(fullCoin.coin.code),
        VolumeMenuType.Currency(currency.code)
    )

    private var volumeType: VolumeMenuType = volumeOptions[0]

    val volumeMenu = Select(volumeType, volumeOptions)

    val stateObservable: BehaviorSubject<DataState<List<MarketTickerItem>>> = BehaviorSubject.create()

    val currency get() = currencyManager.baseCurrency

    var sortType = SortType.HighestVolume
        private set

    fun start() {
        marketKit.marketTickersSingle(fullCoin.coin.uid)
            .subscribeIO({
                marketTickers = it
                emitItems()
            }, {
                stateObservable.onNext(DataState.Error(it))
            }).let {
                disposables.add(it)
            }
    }

    fun stop() {
        disposables.clear()
    }

    fun setSortType(sortType: SortType) {
        this.sortType = sortType

        emitItems()
    }

    fun setVolumeType(volumeType: VolumeMenuType) {
        this.volumeType = volumeType

        emitItems()
    }

    @Synchronized
    private fun emitItems() {
        val sorted = when (sortType) {
            SortType.HighestVolume -> marketTickers.sortedByDescending { it.volume }
            SortType.LowestVolume -> marketTickers.sortedBy { it.volume }
        }

        stateObservable.onNext(DataState.Success(sorted.map { createItem(it) }))
    }

    private fun createItem(marketTicker: MarketTicker): MarketTickerItem {
        val volume = when (volumeType) {
            is VolumeMenuType.Coin -> marketTicker.volume
            is VolumeMenuType.Currency -> marketTicker.volume.multiply(price)
        }

        return MarketTickerItem(
            marketTicker.marketName,
            marketTicker.marketImageUrl,
            marketTicker.base,
            marketTicker.target,
            marketTicker.rate,
            volume,
            volumeType,
            marketTicker.tradeUrl
        )
    }
}
