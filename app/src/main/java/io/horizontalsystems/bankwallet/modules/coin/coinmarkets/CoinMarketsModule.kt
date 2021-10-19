package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object CoinMarketsModule {
    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = CoinMarketsService(fullCoin, App.currencyManager, App.marketKit)
            return CoinMarketsViewModel(service) as T
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
    val volumeType: VolumeType,
)

enum class SortType {
    HighestVolume, LowestVolume
}

enum class VolumeType {
    Coin, Currency
}
