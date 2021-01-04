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
            val service = MarketTopService(App.currencyManager, MarketListTopDataSource(App.xRateManager))
            return MarketTopViewModel(service, listOf(service)) as T
        }

    }

}

enum class Field(@StringRes val titleResId: Int) {
    HighestCap(R.string.Market_Field_HighestCap), LowestCap(R.string.Market_Field_LowestCap),
    HighestLiquidity(R.string.Market_Field_HighestLiquidity), LowestLiquidity(R.string.Market_Field_LowestLiquidity),
    HighestVolume(R.string.Market_Field_HighestVolume), LowestVolume(R.string.Market_Field_LowestVolume),
    HighestPrice(R.string.Market_Field_HighestPrice), LowestPrice(R.string.Market_Field_LowestPrice),
    TopGainers(R.string.RateList_TopWinners), TopLosers(R.string.RateList_TopLosers),
}

enum class Period(@StringRes val titleResId: Int) {
    Period24h(R.string.Market_Period_24h),
    PeriodWeek(R.string.Market_Period_1week),
    PeriodMonth(R.string.Market_Period_1month)
}

data class MarketTopItem(
        val rank: Int,
        val coinCode: String,
        val coinName: String,
        val marketCap: Double,
        val volume: Double,
        val rate: BigDecimal,
        val diff: BigDecimal,
        val liquidity: BigDecimal
)
