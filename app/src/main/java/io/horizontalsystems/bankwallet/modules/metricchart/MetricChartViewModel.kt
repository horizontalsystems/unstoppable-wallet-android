package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeaderItem
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.disposables.CompositeDisposable

val ChartType.stringResId: Int
    get() = when (this) {
        ChartType.TODAY -> R.string.CoinPage_TimeDuration_Today
        ChartType.DAILY -> R.string.CoinPage_TimeDuration_Day
        ChartType.WEEKLY -> R.string.CoinPage_TimeDuration_Week
        ChartType.WEEKLY2 -> R.string.CoinPage_TimeDuration_TwoWeeks
        ChartType.MONTHLY -> R.string.CoinPage_TimeDuration_Month
        ChartType.MONTHLY_BY_DAY -> R.string.CoinPage_TimeDuration_Month
        ChartType.MONTHLY3 -> R.string.CoinPage_TimeDuration_Month3
        ChartType.MONTHLY6 -> R.string.CoinPage_TimeDuration_HalfYear
        ChartType.MONTHLY12 -> R.string.CoinPage_TimeDuration_Year
        ChartType.MONTHLY24 -> R.string.CoinPage_TimeDuration_Year2
    }

val ChartType.viewChartType: ChartView.ChartType
    get() = when (this) {
        ChartType.TODAY -> ChartView.ChartType.TODAY
        ChartType.DAILY -> ChartView.ChartType.DAILY
        ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
        ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
        ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
        ChartType.MONTHLY_BY_DAY -> ChartView.ChartType.MONTHLY
        ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
        ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
        ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
        ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
    }

val ChartView.ChartType.kitChartType: ChartType
    get() = when (this) {
        ChartView.ChartType.TODAY -> ChartType.TODAY
        ChartView.ChartType.DAILY -> ChartType.DAILY
        ChartView.ChartType.WEEKLY -> ChartType.WEEKLY
        ChartView.ChartType.WEEKLY2 -> ChartType.WEEKLY2
        ChartView.ChartType.MONTHLY -> ChartType.MONTHLY_BY_DAY
        ChartView.ChartType.MONTHLY3 -> ChartType.MONTHLY3
        ChartView.ChartType.MONTHLY6 -> ChartType.MONTHLY6
        ChartView.ChartType.MONTHLY12 -> ChartType.MONTHLY12
        ChartView.ChartType.MONTHLY24 -> ChartType.MONTHLY24
    }

class MetricChartViewModel(
    private val service: MetricChartService,
    private val factory: MetricChartFactory,
) : ViewModel() {

    val title by service::title
    val currency by service::currency
    val coinChartViewItemLiveData = MutableLiveData<CoinChartAdapter.ViewItemWrapper>()
    val chartTitleLiveData = MutableLiveData<ChartInfoHeaderItem>()
    val chartTypes = MutableLiveData<List<Pair<ChartView.ChartType, Int>>>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.dataOrNull?.let {
                    syncChartItems(it)
                }
            }
            .let { disposables.add(it) }

        chartTypes.postValue(
            service.chartTypes.map {
                Pair(it.viewChartType, it.stringResId)
            }
        )

        service.updateChartType(service.chartTypes.first())
    }

    private fun syncChartItems(chartItems: Pair<ChartType, List<MetricChartModule.Item>>) {
        val chartViewItem = factory.convert(
            chartItems.second,
            chartItems.first.viewChartType,
            MetricChartModule.ValueType.CompactCurrencyValue,
            service.currency
        )

        val chartInfoHeaderItem = ChartInfoHeaderItem(
            chartViewItem.lastValueWithDiff.value,
            Value.Percent(chartViewItem.lastValueWithDiff.diff)
        )
        chartTitleLiveData.postValue(chartInfoHeaderItem)

        val chartInfoData = ChartInfoData(
            chartViewItem.chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )
        coinChartViewItemLiveData.postValue(CoinChartAdapter.ViewItemWrapper(chartInfoData))
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType.kitChartType)
    }


}
