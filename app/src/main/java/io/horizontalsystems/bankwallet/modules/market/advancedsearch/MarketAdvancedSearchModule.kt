package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R

object MarketAdvancedSearchModule {
    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MarketAdvancedSearchViewModel(MarketAdvancedSearchService()) as T
        }

    }
}

enum class CoinList(val itemsCount: Int, @StringRes val titleResId: Int) {
    Top100(100, R.string.Market_Filter_Top_100),
    Top250(250, R.string.Market_Filter_Top_250),
    Top500(500, R.string.Market_Filter_Top_500),
    Top1000(1000, R.string.Market_Filter_Top_1000),
    Top2500(2500, R.string.Market_Filter_Top_2500),;
}

enum class Range(@StringRes val titleResId: Int) {
    Range_0_1M(R.string.Market_Filter_Range_0_1M),
    Range_1M_10M(R.string.Market_Filter_Range_1M_10M),
    Range_10M_100M(R.string.Market_Filter_Range_10M_100M),
    Range_100M_1B(R.string.Market_Filter_Range_100M_1B),
    Range_1B_10B(R.string.Market_Filter_Range_1B_10B),
    Range_10B_More(R.string.Market_Filter_Range_10B_More),
}

enum class TimePeriod(@StringRes val titleResId: Int) {
    Value_1D(R.string.Market_Filter_TimePeriod_1D),
    Value_1W(R.string.Market_Filter_TimePeriod_1W),
    Value_1M(R.string.Market_Filter_TimePeriod_1M),
    Value_3M(R.string.Market_Filter_TimePeriod_3M),
    Value_6M(R.string.Market_Filter_TimePeriod_6M),
    Value_1Y(R.string.Market_Filter_TimePeriod_1Y),
}

