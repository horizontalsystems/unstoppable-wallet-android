package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

sealed class TopListSortType(@StringRes val titleRes: Int) {

    object MarketCap: TopListSortType(R.string.RateList_MarketCap)
    object Winners: TopListSortType(R.string.RateList_TopWinners)
    object Losers: TopListSortType(R.string.RateList_TopLosers)

}
