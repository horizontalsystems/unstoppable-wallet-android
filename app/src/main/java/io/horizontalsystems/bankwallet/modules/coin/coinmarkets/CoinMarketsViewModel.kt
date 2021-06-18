package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.xrateskit.entities.MarketTicker
import java.math.BigDecimal

class CoinMarketsViewModel(
        coinCode: String,
        private val numberFormatter: IAppNumberFormatter,
        appConfigProvider: IAppConfigProvider
) : ViewModel() {

    val coinMarketItems = MutableLiveData<List<MarketTickerViewItem>>()

    val sortingFields: Array<SortingField> = arrayOf(SortingField.HighestVolume, SortingField.LowestVolume)
    var sortingField: SortingField = sortingFields.first()
        private set

    //MarketTicker has rate field in USD fiat
    private val usdCurrency = appConfigProvider.currencies.first { it.code == "USD" }
    private val viewOptions = listOf(coinCode, usdCurrency.code)
    private var selectedViewOptionId: Int = 0
    private val showInFiat: Boolean
        get() {
            return selectedViewOptionId == 1
        }

    val fieldViewOptions = viewOptions.mapIndexed { index, title ->
        MarketListHeaderView.FieldViewOption(index, title, index == selectedViewOptionId)
    }

    var marketTickers: List<MarketTicker> = listOf()
        set(value) {
            field = value
            syncCoinMarketItems()
        }

    fun update(sortingField: SortingField? = null, fieldViewOptionId: Int? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        fieldViewOptionId?.let {
            this.selectedViewOptionId = it
        }
        syncCoinMarketItems()
    }

    private fun syncCoinMarketItems() {
        val marketTickersSorted = marketTickers.sort(sortingField)
        val viewItems = getCoinMarketItems(marketTickersSorted, showInFiat)
        coinMarketItems.postValue(viewItems)
    }

    private fun getCoinMarketItems(tickers: List<MarketTicker>, showInFiat: Boolean): List<MarketTickerViewItem> {
        return tickers.map { ticker ->
            val subvalue = if (showInFiat) {
                formatFiatShortened(ticker.volume.multiply(ticker.rate), usdCurrency.symbol)
            } else {
                val (shortenValue, suffix) = numberFormatter.shortenValue(ticker.volume)
                "$shortenValue $suffix ${ticker.base}"
            }

            MarketTickerViewItem(
                    ticker.marketName,
                    "${ticker.base}/${ticker.target}",
                    numberFormatter.formatCoin(ticker.rate, ticker.target, 0, 8),
                    subvalue,
                    ticker.imageUrl
            )
        }
    }

    private fun List<MarketTicker>.sort(sortingField: SortingField) = when (sortingField) {
        SortingField.HighestVolume -> sortedByDescendingNullLast { it.volume }
        SortingField.LowestVolume -> sortedByNullLast { it.volume }
        else -> throw IllegalArgumentException()
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }
}
