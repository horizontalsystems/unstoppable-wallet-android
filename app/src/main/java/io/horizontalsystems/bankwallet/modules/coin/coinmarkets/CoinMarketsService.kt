package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketTicker
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class CoinMarketsService(
    val fullCoin: FullCoin,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var marketTickers = listOf<MarketTicker>()
    private val price = marketKit.coinPrice(fullCoin.coin.uid, currencyManager.baseCurrency.code)?.value ?: BigDecimal.ZERO

    private var verifiedType: VerifiedType = VerifiedType.All

    val verifiedMenu = Select(verifiedType, VerifiedType.values().toList())
    val stateObservable: BehaviorSubject<DataState<List<MarketTickerItem>>> = BehaviorSubject.create()

    val currency get() = currencyManager.baseCurrency


    fun start() {
        coroutineScope.launch {
            try {
                val marketTickers = marketKit.marketTickersSingle(fullCoin.coin.uid).await()
                this@CoinMarketsService.marketTickers = marketTickers.sortedByDescending { it.volume }
                emitItems()
            } catch (e: Throwable) {
                stateObservable.onNext(DataState.Error(e))
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
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
