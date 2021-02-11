package io.horizontalsystems.bankwallet.modules.ratechart

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.xrateskit.entities.ChartType

class RateChartView : RateChartModule.View {
    val showSpinner = SingleLiveEvent<Unit>()
    val hideSpinner = SingleLiveEvent<Unit>()
    val setDefaultMode = SingleLiveEvent<ChartType>()
    val setSelectedPoint = SingleLiveEvent<ChartPointViewItem>()
    val showChartInfo = SingleLiveEvent<ChartInfoViewItem>()
    val showMarketInfo = SingleLiveEvent<MarketInfoViewItem>()
    val showError = SingleLiveEvent<Throwable>()
    val showEma = SingleLiveEvent<Boolean>()
    val showMacd = SingleLiveEvent<Boolean>()
    val showRsi = SingleLiveEvent<Boolean>()
    val alertNotificationUpdated = SingleLiveEvent<Unit>()
    val showNotificationMenu = SingleLiveEvent<Pair<String, String>>()
    val isFavorite = MutableLiveData<Boolean>()

    override fun showSpinner() {
        showSpinner.call()
    }

    override fun hideSpinner() {
        hideSpinner.call()
    }

    override fun setChartType(type: ChartType) {
        setDefaultMode.postValue(type)
    }

    override fun showChartInfo(viewItem: ChartInfoViewItem) {
        showChartInfo.postValue(viewItem)
    }

    override fun showMarketInfo(viewItem: MarketInfoViewItem) {
        showMarketInfo.postValue(viewItem)
    }

    override fun showSelectedPointInfo(item: ChartPointViewItem) {
        setSelectedPoint.postValue(item)
    }

    override fun showError(ex: Throwable) {
        showError.postValue(ex)
    }

    override fun setEmaEnabled(enabled: Boolean) {
        showEma.postValue(enabled)
    }

    override fun setMacdEnabled(enabled: Boolean) {
        showMacd.postValue(enabled)
    }

    override fun setRsiEnabled(enabled: Boolean) {
        showRsi.postValue(enabled)
    }

    override fun notificationIconUpdated() {
        alertNotificationUpdated.call()
    }

    override fun openNotificationMenu(coinId: String, coinName: String) {
        showNotificationMenu.postValue(Pair(coinId, coinName))
    }

    override fun setIsFavorite(value: Boolean) {
        isFavorite.postValue(value)
    }
}
