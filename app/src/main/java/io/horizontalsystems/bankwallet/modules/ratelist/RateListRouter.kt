package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.core.SingleLiveEvent

class RateListRouter : RateListModule.IRouter {

    var openChartLiveEvent = SingleLiveEvent<Pair<String, String>>()

    override fun openChart(coinCode: String, coinTitle: String) {
        openChartLiveEvent.postValue(Pair(coinCode, coinTitle))
    }
}
