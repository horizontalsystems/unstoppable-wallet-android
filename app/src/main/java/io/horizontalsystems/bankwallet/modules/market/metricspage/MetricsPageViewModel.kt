package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.metricchart.ChartViewItem
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartService
import io.horizontalsystems.chartview.ChartView
import io.reactivex.disposables.CompositeDisposable

class MetricsPageViewModel(
    private val chartConfiguration: MetricChartModule.IMetricChartConfiguration,
    private val metricsService: MetricChartService,
    private val factory: MetricChartFactory,
    private val clearables: List<Clearable>
) : ViewModel() {

    val subtitleLiveData = MutableLiveData<MetricsPageSubtitleAdapter.ViewItemWrapper>()
    val chartInfoLiveData = MutableLiveData<CoinChartAdapter.ViewItemWrapper>()

    var chartType: ChartView.ChartType = ChartView.ChartType.DAILY

    val currency = metricsService.currency

    private val disposables = CompositeDisposable()
    private var chartInfoData: ChartInfoData? = null
    private var showChartSpinner: Boolean = false
    private var showChartError: Boolean = false


    init {
        fetchChartInfo()

        metricsService.stateObservable
            .subscribeIO {
                sync(it)
            }
            .let {
                disposables.add(it)
            }
    }

    fun onChartTypeSelect(chartType: ChartView.ChartType) {
        this.chartType = chartType
        fetchChartInfo()
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    private fun sync(dataState: DataState<List<MetricChartModule.Item>>) {
        showChartSpinner = dataState.loading
        showChartError = dataState.errorOrNull != null

        val data = dataState.dataOrNull

        if (data == null) {
            updateChart()
            return
        }

        val chartViewItem =
            factory.convert(data, chartType, chartConfiguration.valueType, metricsService.currency)

        subtitleLiveData.postValue(
            MetricsPageSubtitleAdapter.ViewItemWrapper(
                chartViewItem.lastValueWithDiff?.value,
                chartViewItem.lastValueWithDiff?.diff
            )
        )

        chartInfoData = getChartInfoData(chartViewItem)

        updateChart()
    }

    private fun updateChart() {
        val chartInfoWrapper = CoinChartAdapter.ViewItemWrapper(
            chartInfoData,
            showChartSpinner,
            showChartError
        )

        chartInfoLiveData.postValue(chartInfoWrapper)
    }

    private fun getChartInfoData(chartViewItem: ChartViewItem): ChartInfoData? {
        val chartData = chartViewItem.chartData ?: return null
        return ChartInfoData(
            chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )
    }

    private fun fetchChartInfo() {
        metricsService.updateChartInfo(chartType)
    }

}
