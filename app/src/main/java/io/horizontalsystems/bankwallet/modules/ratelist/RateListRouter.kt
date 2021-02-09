package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent

class RateListRouter : RateListModule.IRouter {

    var openChartLiveEvent = SingleLiveEvent<Pair<String, String>>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<TopListSortType>()

    override fun openChart(coinCode: String, coinTitle: String) {
        openChartLiveEvent.postValue(Pair(coinCode, coinTitle))
    }

    override fun openSortingTypeDialog(sortType: TopListSortType) {
        openSortingTypeDialogLiveEvent.postValue(sortType)
    }
}
