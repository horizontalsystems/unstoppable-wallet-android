package cash.p.terminal.modules.coin.coinmarkets

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object CoinMarketsModule {
    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinMarketsService(fullCoin, App.currencyManager, App.marketKit)
            return CoinMarketsViewModel(service) as T
        }
    }

    @Immutable
    data class Menu(
        val sortDescending: Boolean,
        val marketFieldSelect: Select<MarketField>
    )

    sealed class VolumeMenuType : WithTranslatableTitle {
        class Coin(val name: String) : VolumeMenuType()
        class Currency(val name: String) : VolumeMenuType()

        override val title: TranslatableString
            get() = when (this) {
                is Coin -> TranslatableString.PlainString(name)
                is Currency -> TranslatableString.PlainString(name)
            }
    }
}

data class MarketTickerItem(
    val market: String,
    val marketImageUrl: String?,
    val baseCoinCode: String,
    val targetCoinCode: String,
    val rate: BigDecimal,
    val volume: BigDecimal,
    val volumeType: CoinMarketsModule.VolumeMenuType,
    val tradeUrl: String?
)

enum class SortType {
    HighestVolume, LowestVolume;

    fun next() = values()[if (ordinal == values().size - 1) 0 else ordinal + 1]
}
