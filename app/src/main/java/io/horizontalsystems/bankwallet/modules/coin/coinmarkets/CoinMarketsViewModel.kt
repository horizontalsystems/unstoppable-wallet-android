package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketTicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class CoinMarketsViewModel(
    private val fullCoin: FullCoin,
    private val baseCurrency: Currency,
    private val marketKit: MarketKitWrapper,
) : ViewModelUiState<CoinMarketsModule.CoinMarketUiState>() {

    private var marketTickers = listOf<MarketTicker>()
    private var filteredMarketTickers = listOf<MarketTicker>()
    private var viewState: ViewState = ViewState.Loading
    private var cexDexMenu: Select<CoinMarketsModule.ExchangeType> =
        Select(
            CoinMarketsModule.ExchangeType.ALL,
            CoinMarketsModule.ExchangeType.entries,
        )

    var verified: Boolean = false
        private set


    init {
        syncMarketTickers()
    }

    override fun createState() = CoinMarketsModule.CoinMarketUiState(
        verified = verified,
        viewState = viewState,
        exchangeTypeMenu = cexDexMenu,
        items = filteredMarketTickers.map { createViewItem(it) }
    )

    private fun syncMarketTickers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tickers =
                    marketKit.marketTickersSingle(fullCoin.coin.uid, baseCurrency.code).await()
                marketTickers = tickers.sortedByDescending { it.volume }
                filterMarketTickers()
                viewState = ViewState.Success
            } catch (e: Throwable) {
                viewState = ViewState.Error(e)
            }
            emitState()
        }
    }

    private fun filterMarketTickers() {
        filteredMarketTickers = marketTickers.filter { ticker ->
            val matchesExchangeType = when (cexDexMenu.selected) {
                CoinMarketsModule.ExchangeType.CEX -> ticker.centralized
                CoinMarketsModule.ExchangeType.DEX -> !ticker.centralized
                CoinMarketsModule.ExchangeType.ALL -> true
            }

            val matchesVerification = !verified || ticker.verified

            matchesExchangeType && matchesVerification
        }

        emitState()
    }

    private fun createViewItem(item: MarketTicker): MarketTickerViewItem {
        return MarketTickerViewItem(
            item.marketName,
            item.marketImageUrl,
            "${item.base}/${item.target}",
            App.numberFormatter.formatFiatShort(
                item.fiatVolume,
                baseCurrency.symbol,
                baseCurrency.decimal
            ),
            App.numberFormatter.formatCoinShort(item.volume, item.base, 8),
            item.tradeUrl,
            if (item.verified) TranslatableString.ResString(R.string.CoinPage_MarketsLabel_Verified) else null
        )
    }

    fun onErrorClick() {
        syncMarketTickers()
    }

    fun setExchangeType(type: CoinMarketsModule.ExchangeType) {
        cexDexMenu = Select(type, CoinMarketsModule.ExchangeType.entries)
        filterMarketTickers()
        emitState()
    }

    fun setVerified(verified: Boolean) {
        this.verified = verified
        filterMarketTickers()
        emitState()
    }

}
