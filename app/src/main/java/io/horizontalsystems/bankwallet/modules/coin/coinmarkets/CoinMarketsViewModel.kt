package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.MarketTicker
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.ui.compose.components.ToggleIndicator
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import java.math.BigDecimal

class CoinMarketsViewModel(
    private val coinCode: String,
    coinUid: String,
    currencyManager: ICurrencyManager,
    marketKit: MarketKit,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private val baseCurrency = currencyManager.baseCurrency
    private val marketRate = marketKit.coinPrice(coinUid, baseCurrency.code)?.value ?: BigDecimal.ONE
    private var showInFiat = false
    private var sortDesc = true
    private val toggleButton: MarketListHeaderView.ToggleButton
        get() {
            return MarketListHeaderView.ToggleButton(
                title = if (showInFiat) baseCurrency.code else coinCode,
                indicators = listOf(ToggleIndicator(!showInFiat), ToggleIndicator(showInFiat))
            )
        }

    private val sortMenu: MarketListHeaderView.SortMenu
        get() {
            val direction =
                if (sortDesc) MarketListHeaderView.Direction.Down else MarketListHeaderView.Direction.Up
            return MarketListHeaderView.SortMenu.DuoOption(direction)
        }

    val topMenuLiveData = MutableLiveData(Pair(sortMenu, toggleButton))
    val coinMarketItems = MutableLiveData<Pair<List<MarketTickerViewItem>, Boolean>>()

    var marketTickers: List<MarketTicker> = listOf()
        set(value) {
            field = value
            syncCoinMarketItems(false)
        }

    fun onChangeSorting() {
        sortDesc = !sortDesc
        syncCoinMarketItems(true)
        updateTopMenu()
    }

    fun onToggleButtonClick() {
        showInFiat = !showInFiat
        syncCoinMarketItems(false)
        updateTopMenu()
    }

    private fun updateTopMenu(){
        topMenuLiveData.postValue(Pair(sortMenu, toggleButton))
    }

    private fun syncCoinMarketItems(scrollToTop: Boolean) {
        val marketTickersSorted = marketTickers.sort(sortDesc)
        val viewItems = getCoinMarketItems(marketTickersSorted, showInFiat)
        coinMarketItems.postValue(Pair(viewItems, scrollToTop))
    }

    private fun getCoinMarketItems(
        tickers: List<MarketTicker>,
        showInFiat: Boolean
    ): List<MarketTickerViewItem> {
        return tickers.map { ticker ->
            val subValue = if (showInFiat) {
                formatFiatShortened(ticker.volume.multiply(marketRate), baseCurrency.symbol)
            } else {
                val (shortenValue, suffix) = numberFormatter.shortenValue(ticker.volume)
                "$shortenValue $suffix ${ticker.base}"
            }

            MarketTickerViewItem(
                ticker.marketName,
                "${ticker.base}/${ticker.target}",
                numberFormatter.formatCoin(ticker.rate, ticker.target, 0, 8),
                subValue,
                ticker.imageUrl
            )
        }
    }

    private fun List<MarketTicker>.sort(sortDesc: Boolean) =
        if (sortDesc) {
            sortedByDescendingNullLast { it.volume }
        } else {
            sortedByNullLast { it.volume }
        }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(
            shortCapValue.first,
            symbol,
            0,
            2
        ) + " " + shortCapValue.second
    }
}
