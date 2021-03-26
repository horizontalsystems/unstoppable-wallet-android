package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.xrateskit.entities.TimePeriod as XRatesKitTimePeriod

object MarketAdvancedSearchModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketAdvancedSearchService(App.xRateManager, App.currencyManager)
            return MarketAdvancedSearchViewModel(service, listOf(service)) as T
        }

    }
}

enum class CoinList(val itemsCount: Int, @StringRes val titleResId: Int) {
    Top100(100, R.string.Market_Filter_Top_100),
    Top250(250, R.string.Market_Filter_Top_250),
    Top500(500, R.string.Market_Filter_Top_500),
    Top1000(1000, R.string.Market_Filter_Top_1000),
    Top1500(1500, R.string.Market_Filter_Top_1500),
}

enum class Range(@StringRes val titleResId: Int, val values: Pair<Long?, Long?>) {
    Range_0_5M(R.string.Market_Filter_Range_0_5M, Pair(null, 5_000_000)),
    Range_5M_20M(R.string.Market_Filter_Range_5M_20M, Pair(5_000_000, 20_000_000)),
    Range_20M_100M(R.string.Market_Filter_Range_20M_100M, Pair(20_000_000, 100_000_000)),
    Range_100M_1B(R.string.Market_Filter_Range_100M_1B, Pair(100_000_000, 1_000_000_000)),
    Range_1B_5B(R.string.Market_Filter_Range_1B_5B, Pair(1_000_000_000, 5_000_000_000)),
    Range_5B_More(R.string.Market_Filter_Range_5B_More, Pair(5_000_000_000, null)),
}

enum class TimePeriod(@StringRes val titleResId: Int, val xRatesKitTimePeriod: XRatesKitTimePeriod) {
    TimePeriod_1D(R.string.Market_Filter_TimePeriod_1D, XRatesKitTimePeriod.HOUR_24),
    TimePeriod_1W(R.string.Market_Filter_TimePeriod_1W, XRatesKitTimePeriod.DAY_7),
    TimePeriod_2W(R.string.Market_Filter_TimePeriod_2W, XRatesKitTimePeriod.DAY_14),
    TimePeriod_1M(R.string.Market_Filter_TimePeriod_1M, XRatesKitTimePeriod.DAY_30),
    TimePeriod_6M(R.string.Market_Filter_TimePeriod_6M, XRatesKitTimePeriod.DAY_200),
    TimePeriod_1Y(R.string.Market_Filter_TimePeriod_1Y, XRatesKitTimePeriod.YEAR_1),
}

enum class PriceChange(@StringRes val titleResId: Int, @ColorRes val colorResId: Int, val values: Pair<Long?, Long?>) {
    Positive_10_plus(R.string.Market_Filter_PriceChange_Positive_10_plus, R.color.remus, Pair(10, null)),
    Positive_25_plus(R.string.Market_Filter_PriceChange_Positive_25_plus, R.color.remus, Pair(25, null)),
    Positive_50_plus(R.string.Market_Filter_PriceChange_Positive_50_plus, R.color.remus, Pair(50, null)),
    Positive_100_plus(R.string.Market_Filter_PriceChange_Positive_100_plus, R.color.remus, Pair(100, null)),
    Negative_10_minus(R.string.Market_Filter_PriceChange_Negative_10_minus, R.color.lucian, Pair(null, -10)),
    Negative_25_minus(R.string.Market_Filter_PriceChange_Negative_25_minus, R.color.lucian, Pair(null, -25)),
    Negative_50_minus(R.string.Market_Filter_PriceChange_Negative_50_minus, R.color.lucian, Pair(null, -50)),
    Negative_100_minus(R.string.Market_Filter_PriceChange_Negative_100_minus, R.color.lucian, Pair(null, -100)),
}

