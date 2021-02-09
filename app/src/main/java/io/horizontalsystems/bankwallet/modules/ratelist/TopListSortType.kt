package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

sealed class TopListSortType(@StringRes val titleRes: Int) {

    object Rank: TopListSortType(R.string.RateList_Rank)
    object Winners: TopListSortType(R.string.RateList_TopGainers)
    object Losers: TopListSortType(R.string.RateList_TopLosers)

}
