package io.horizontalsystems.bankwallet.modules.market.top

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal

object MarketTopModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketTopService(App.currencyManager, MarketListTopDataSource(App.xRateManager), App.xRateManager)
            return MarketTopViewModel(service, App.connectivityManager, listOf(service)) as T
        }

    }

}

enum class SortingField(@StringRes val titleResId: Int) {
    HighestCap(R.string.Market_Field_HighestCap), LowestCap(R.string.Market_Field_LowestCap),
    HighestVolume(R.string.Market_Field_HighestVolume), LowestVolume(R.string.Market_Field_LowestVolume),
    HighestPrice(R.string.Market_Field_HighestPrice), LowestPrice(R.string.Market_Field_LowestPrice),
    TopGainers(R.string.RateList_TopWinners), TopLosers(R.string.RateList_TopLosers),
}

enum class MarketField(@StringRes val titleResId: Int) {
    MarketCap(R.string.Market_Field_MarketCap),
    Volume(R.string.Market_Field_Volume),
    PriceDiff(R.string.Market_Field_PriceDiff)
}

data class MarketTopItem(
        val rank: Int,
        val coinCode: String,
        val coinName: String,
        val volume: BigDecimal,
        val rate: BigDecimal,
        val diff: BigDecimal,
        val marketCap: BigDecimal?
)
