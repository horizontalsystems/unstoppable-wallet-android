package io.horizontalsystems.bankwallet.modules.market.topcoins

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.bankwallet.modules.market.overview.TopMarketsRepository
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object MarketTopCoinsModule {

    class Factory(
        private val topMarket: TopMarket? = null,
        private val sortingField: SortingField? = null,
        private val marketField: MarketField? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val topMarketsRepository = TopMarketsRepository(App.marketKit)
            val service = MarketTopCoinsService(
                topMarketsRepository,
                App.currencyManager,
                topMarket ?: defaultTopMarket,
                sortingField ?: defaultSortingField
            )
            return MarketTopCoinsViewModel(service, marketField ?: defaultMarketField) as T
        }

        companion object {
            val defaultSortingField = SortingField.HighestCap
            val defaultTopMarket = TopMarket.Top250
            val defaultMarketField = MarketField.MarketCap
        }
    }

    data class Header(
        val title: String,
        val description: String,
        val icon: ImageSource,
    )

    data class Menu(
        val sortingFieldSelect: Select<SortingField>,
        val topMarketSelect: Select<TopMarket>?,
        val marketFieldSelect: Select<MarketField>
    )

    sealed class ViewItemState {
        class Error(val error: String) : ViewItemState()
        class Data(val items: List<MarketViewItem>) : ViewItemState()
    }

    @Immutable
    data class MarketViewItem(
        val fullCoin: FullCoin,
        val coinRate: String,
        val marketDataValue: MarketDataValue,
        val rank: String?,
    ) {

        companion object {
            fun create(
                marketItem: MarketItem,
                currency: Currency,
                marketField: MarketField
            ): MarketViewItem {
                val marketDataValue = when (marketField) {
                    MarketField.MarketCap -> {
                        val (shortenValue, suffix) = App.numberFormatter.shortenValue(
                            marketItem.marketCap.value
                        )
                        val marketCapFormatted = App.numberFormatter.formatFiat(
                            shortenValue,
                            currency.symbol,
                            0,
                            2
                        ) + " $suffix"

                        MarketDataValue.MarketCap(marketCapFormatted)
                    }
                    MarketField.Volume -> {
                        val (shortenValue, suffix) = App.numberFormatter.shortenValue(
                            marketItem.volume.value
                        )
                        val volumeFormatted = App.numberFormatter.formatFiat(
                            shortenValue,
                            currency.symbol,
                            0,
                            2
                        ) + " $suffix"

                        MarketDataValue.Volume(volumeFormatted)
                    }
                    MarketField.PriceDiff -> MarketDataValue.Diff(marketItem.diff)
                }
                return MarketViewItem(
                    marketItem.fullCoin,
                    App.numberFormatter.formatFiat(marketItem.rate.value, currency.symbol, 0, 6),
                    marketDataValue,
                    marketItem.fullCoin.coin.marketCapRank.toString()
                )
            }
        }
    }

}

sealed class MarketDataValue {
    class MarketCap(val value: String) : MarketDataValue()
    class Volume(val value: String) : MarketDataValue()
    class Diff(val value: BigDecimal?) : MarketDataValue()
}
