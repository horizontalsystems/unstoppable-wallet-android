package cash.p.terminal.modules.coin.coinmarkets

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.ui.compose.Select
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

    private var verifiedType: VerifiedType = VerifiedType.All

    val verifiedMenu = Select(verifiedType, VerifiedType.values().toList())
    val stateObservable: BehaviorSubject<DataState<List<MarketTickerItem>>> = BehaviorSubject.create()

    val currency get() = currencyManager.baseCurrency


    fun start() {
        marketKit.marketTickersSingle(fullCoin.coin.uid)
            .subscribeIO({ marketTickers ->
                this.marketTickers = marketTickers.sortedByDescending { it.volume }
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

    fun setVerifiedType(verifiedType: VerifiedType) {
        this.verifiedType = verifiedType

        emitItems()
    }

    @Synchronized
    private fun emitItems() {
        val filtered = when (verifiedType) {
            VerifiedType.Verified -> marketTickers.filter { it.verified }
            VerifiedType.All -> marketTickers
        }

        stateObservable.onNext(DataState.Success(filtered.map { createItem(it) }))
    }

    private fun createItem(marketTicker: MarketTicker): MarketTickerItem = MarketTickerItem(
        market = marketTicker.marketName,
        marketImageUrl = marketTicker.marketImageUrl,
        baseCoinCode = marketTicker.base,
        targetCoinCode = marketTicker.target,
        volumeFiat = marketTicker.volume.multiply(price),
        volumeToken = marketTicker.volume,
        tradeUrl = marketTicker.tradeUrl,
        verified = marketTicker.verified
    )
}
