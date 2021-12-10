package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.stringResId
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

interface XxxChartService {
    val currency: Currency
    val chartTypeObservable: Observable<ChartView.ChartType>
    val chartTypes: List<ChartView.ChartType>
    val chartItemsObservable: Observable<DataState<Pair<ChartView.ChartType, List<MetricChartModule.Item>>>>
    fun updateChartType(chartType: ChartView.ChartType)
}

class XxxChart(private val service: XxxChartService, private val factory: MetricChartFactory) {
    val currentValueLiveData = MutableLiveData<String>()
    val currentValueDiffLiveData = MutableLiveData<Value.Percent>()
    val chartTabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val chartInfoLiveData = MutableLiveData<ChartInfoData>()
    val chartLoadingLiveData = MutableLiveData<Boolean>()
    val chartViewStateLiveData = MutableLiveData<ViewState>()
    val currency by service::currency

    private val disposables = CompositeDisposable()

    fun start() {
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
            .let { disposables.add(it) }
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType)
    }

    private fun syncChartItems(chartType: ChartView.ChartType, chartItems: List<MetricChartModule.Item>) {
        chartItems.lastOrNull()?.let { lastItem ->
            val lastItemValue = lastItem.value
            val currentValue = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))
            currentValueLiveData.postValue(currentValue)

            val firstItemValue = chartItems.first().value
            val currentValueDiff = Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal())
            currentValueDiffLiveData.postValue(currentValueDiff)
        }

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

        chartInfoLiveData.postValue(chartInfoData)
    }

    fun stop() {
        disposables.clear()
    }
}
