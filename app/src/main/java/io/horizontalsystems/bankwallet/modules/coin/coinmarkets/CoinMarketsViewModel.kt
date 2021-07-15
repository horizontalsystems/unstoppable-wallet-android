package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.MarketTicker
import java.math.BigDecimal

class CoinMarketsViewModel(
    coinCode: String,
    coinType: CoinType,
    currencyManager: ICurrencyManager,
    rateManager: IRateManager,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    val coinMarketItems = MutableLiveData<Pair<List<MarketTickerViewItem>, Boolean>>()

    val sortingFields: Array<SortingField> = arrayOf(SortingField.HighestVolume, SortingField.LowestVolume)
    var sortingField: SortingField = sortingFields.first()
        private set

    private val baseCurrency = currencyManager.baseCurrency
    private val viewOptions = listOf(coinCode, baseCurrency.code)
    private var selectedViewOptionId: Int = 0
    private val marketRate = rateManager.getLatestRate(coinType, baseCurrency.code) ?: BigDecimal.ONE
    private val showInFiat: Boolean
        get() = selectedViewOptionId == 1

    val fieldViewOptions = viewOptions.mapIndexed { index, title ->
        MarketListHeaderView.FieldViewOption(index, title, index == selectedViewOptionId)
    }

    var marketTickers: List<MarketTicker> = listOf()
        set(value) {
            field = value
            syncCoinMarketItems(false)
        }

    fun update(sortField: SortingField? = null, fieldViewOptionId: Int? = null, scrollToTop: Boolean) {
        sortField?.let {
            sortingField = it
        }

        fieldViewOptionId?.let {
            selectedViewOptionId = it
        }

        syncCoinMarketItems(scrollToTop)
    }

    private fun syncCoinMarketItems(scrollToTop: Boolean) {
        val marketTickersSorted = marketTickers.sort(sortingField)
        val viewItems = getCoinMarketItems(marketTickersSorted, showInFiat)
        coinMarketItems.postValue(Pair(viewItems, scrollToTop))
    }

    private fun getCoinMarketItems(tickers: List<MarketTicker>, showInFiat: Boolean): List<MarketTickerViewItem> {
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
