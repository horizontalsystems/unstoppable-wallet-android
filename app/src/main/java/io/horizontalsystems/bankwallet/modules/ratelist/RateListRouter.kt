package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent

class RateListRouter : RateListModule.IRouter {

    var openChartLiveEvent = SingleLiveEvent<Triple<String, String, CoinType?>>()
    val openSortingTypeDialogLiveEvent = SingleLiveEvent<TopListSortType>()

    override fun openChart(coinCode: String, coinTitle: String, coinType: CoinType?) {
        openChartLiveEvent.postValue(Triple(coinCode, coinTitle, coinType))
    }

    override fun openSortingTypeDialog(sortType: TopListSortType) {
        openSortingTypeDialogLiveEvent.postValue(sortType)
    }
}
