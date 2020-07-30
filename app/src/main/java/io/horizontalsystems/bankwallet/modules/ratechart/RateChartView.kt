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
    val alertNotificationActive = MutableLiveData<Boolean>()
    val alertNotificationVisible = MutableLiveData<Boolean>()

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

    override fun setEmaEnabled(visible: Boolean) {
        showEma.postValue(visible)
    }

    override fun setMacdEnabled(visible: Boolean) {
        showMacd.postValue(visible)
    }

    override fun setRsiEnabled(visible: Boolean) {
        showRsi.postValue(visible)
    }

    override fun setAlertNotificationActive(active: Boolean) {
        alertNotificationActive.postValue(active)
    }

    override fun showNotificationIcon(visible: Boolean) {
        alertNotificationVisible.postValue(visible)
    }
}
