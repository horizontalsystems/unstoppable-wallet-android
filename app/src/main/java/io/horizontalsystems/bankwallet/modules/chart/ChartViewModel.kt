package io.horizontalsystems.bankwallet.modules.chart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.stringResId
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartView
import io.reactivex.disposables.CompositeDisposable

class ChartViewModel(private val service: ChartService, private val factory: MetricChartFactory) : ViewModel() {
    val chartTabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val chartDataWrapperLiveData = MutableLiveData<ChartDataWrapper>()
    val chartLoadingLiveData = MutableLiveData<Boolean>()
    val chartViewStateLiveData = MutableLiveData<ViewState>()
    val currency by service::currency

    private val disposables = CompositeDisposable()

    init {
        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartTypes.map {
                    TabItem(Translator.getString(it.stringResId), it == chartType, it)
                }
                chartTabItemsLiveData.postValue(tabItems)
            }
            .let {
                disposables.add(it)
            }

        service.chartItemsObservable
            .subscribeIO { chartItemsDataState ->
                chartViewStateLiveData.postValue(chartItemsDataState.viewState)

                chartLoadingLiveData.postValue(chartItemsDataState.loading)

                chartItemsDataState.dataOrNull?.let { (chartType, chartItems) ->
                    syncChartItems(chartType, chartItems)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType)
    }

    private fun syncChartItems(
        chartType: ChartView.ChartType,
        chartItems: List<MetricChartModule.Item>,
    ) {
        if (chartItems.isEmpty()) return

        val lastItemValue = chartItems.last().value
        val currentValue = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))

        val firstItemValue = chartItems.first().value
        val currentValueDiff = Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal())

        val chartViewItem = factory.convert(
            chartItems,
            chartType,
            MetricChartModule.ValueType.CompactCurrencyValue,
            service.currency
        )
        val chartInfoData = ChartInfoData(
            chartViewItem.chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )

        chartDataWrapperLiveData.postValue(ChartDataWrapper(currentValue, currentValueDiff, chartInfoData))
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}
