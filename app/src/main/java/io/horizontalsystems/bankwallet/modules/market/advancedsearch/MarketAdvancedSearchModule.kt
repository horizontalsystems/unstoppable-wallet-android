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

enum class MarketCap(@StringRes val titleResId: Int) {
    MarketCap_0_1M(R.string.Market_Filter_MarketCap_0_1M),
    MarketCap_1M_10M(R.string.Market_Filter_MarketCap_1M_10M),
    MarketCap_10M_100M(R.string.Market_Filter_MarketCap_10M_100M),
    MarketCap_100M_1B(R.string.Market_Filter_MarketCap_100M_1B),
    MarketCap_1B_10B(R.string.Market_Filter_MarketCap_1B_10B),
    MarketCap_10B_More(R.string.Market_Filter_MarketCap_10B_More),
}