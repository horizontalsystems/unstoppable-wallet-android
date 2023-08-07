package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.marketkit.models.HsPointTimePeriod

object TechnicalIndicatorsDetailsModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val coinUid: String, private val period: HsPointTimePeriod) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val factory = CoinIndicatorViewItemFactory()
            val service = TechnicalIndicatorService(coinUid, App.marketKit, App.currencyManager)
            return TechnicalIndicatorsDetailsViewModel(service, factory, period) as T
        }
    }

    data class UiState(
        val sections: List<SectionViewItem>,
        val viewState: ViewState?,
    )

    data class SectionViewItem(
        val title: String,
        val details: List<DetailViewItem>,
    )

    data class DetailViewItem(
        val title: String,
        val advice: Advice,
    )
}

data class TechnicalIndicatorData(
    val title: String,
    val advice: AdviceViewItem,
    val blocks: List<AdviceBlock>
)

data class AdviceBlock(
    val type: AdviceViewItem,
    val filled: Boolean
)

enum class Advice {
    SELL, NEUTRAL, BUY, NODATA;

    val rating: Int
        get() = when (this) {
            NODATA, NEUTRAL -> 0
            BUY -> 1
            SELL -> -1
        }

    val color: Color
        get() = when (this) {
            SELL -> Color(0xFFF4503A)
            NEUTRAL -> Color(0xFFFFA800)
            BUY -> Color(0xFFB5C405)
            NODATA -> Color(0xFF808085)
        }

    val title: Int
        get() = when (this) {
            SELL -> R.string.Coin_Analytics_Indicators_Sell
            NEUTRAL -> R.string.Coin_Analytics_Indicators_Neutral
            BUY -> R.string.Coin_Analytics_Indicators_Buy
            NODATA -> R.string.Coin_Analytics_Indicators_NoData
        }
}

enum class AdviceViewItem {
    STRONGSELL, SELL, NEUTRAL, BUY, STRONGBUY;

    val color: Color
        get() = when (this) {
            STRONGSELL -> Color(0xFFF43A4F)
            SELL -> Color(0xFFF4503A)
            NEUTRAL -> Color(0xFFFFA800)
            BUY -> Color(0xFFB5C405)
            STRONGBUY -> Color(0xFF05C46B)
        }

    val color20: Color
        get() = when (this) {
            STRONGSELL -> Color(0x33F43A4F)
            SELL -> Color(0x33F4503A)
            NEUTRAL -> Color(0x33FFA800)
            BUY -> Color(0x33B5C405)
            STRONGBUY -> Color(0x3305C46B)
        }

    val title: Int
        get() = when (this) {
            STRONGSELL -> R.string.Coin_Analytics_Indicators_StrongSell
            SELL -> R.string.Coin_Analytics_Indicators_Sell
            NEUTRAL -> R.string.Coin_Analytics_Indicators_Neutral
            BUY -> R.string.Coin_Analytics_Indicators_Buy
            STRONGBUY -> R.string.Coin_Analytics_Indicators_StrongBuy
        }
}