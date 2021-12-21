package io.horizontalsystems.bankwallet.modules.chart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.reactivex.disposables.CompositeDisposable

class ChartViewModel(private val service: AbstractChartService, private val factory: MetricChartFactory) : ViewModel() {
    val tabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val indicatorsLiveData = MutableLiveData<List<TabItem<ChartIndicator>>>()
    val dataWrapperLiveData = MutableLiveData<ChartDataWrapper>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val viewStateLiveData = MutableLiveData<ViewState>()
    val currency by service::currency

    private val disposables = CompositeDisposable()

    init {
        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartTypes.map {
                    TabItem(Translator.getString(it.stringResId), it == chartType, it)
                }
                tabItemsLiveData.postValue(tabItems)
            }
            .let {
                disposables.add(it)
            }

        service.indicatorObservable
            .subscribeIO { indicator ->
                val indicators = service.chartIndicators.map {
                    TabItem(Translator.getString(it.stringResId), it == indicator.orElse(null), it)
                }
                indicatorsLiveData.postValue(indicators)
            }
            .let {
                disposables.add(it)
            }

        service.chartDataXxxObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.viewState?.let {
                    viewStateLiveData.postValue(it)
                }

                loadingLiveData.postValue(chartItemsDataState.loading)

                chartItemsDataState.dataOrNull?.let {
//                    Log.e("AAA", "ChartViewModel: chartItems size ${chartItems.size}, lastPoint: ${chartItems.lastOrNull()}")

                    syncChartItems(it)
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

    fun onSelectIndicator(chartIndicator: ChartIndicator?) {
        service.updateIndicator(chartIndicator)
    }

    private fun syncChartItems(chartDataXxx: ChartDataXxx) {
        val chartItems = chartDataXxx.items
        if (chartItems.isEmpty()) return

        val lastItemValue = chartItems.last().value
        val currentValue = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))

        val firstItemValue = chartItems.first().value
        val currentValueDiff = Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal())

        val chartViewItem = factory.convert(
            MetricChartModule.ValueType.CompactCurrencyValue,
            service.currency,
            chartDataXxx
        )
        val chartInfoData = ChartInfoData(
            chartViewItem.chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )

        dataWrapperLiveData.postValue(ChartDataWrapper(currentValue, currentValueDiff, chartInfoData))
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}

private val ChartIndicator.stringResId: Int
    get() = when (this) {
        ChartIndicator.Ema -> R.string.CoinPage_IndicatorEMA
        ChartIndicator.Macd -> R.string.CoinPage_IndicatorMACD
        ChartIndicator.Rsi -> R.string.CoinPage_IndicatorRSI
    }

val ChartView.ChartType.stringResId: Int
    get() = when (this) {
        ChartView.ChartType.TODAY -> R.string.CoinPage_TimeDuration_Today
        ChartView.ChartType.DAILY -> R.string.CoinPage_TimeDuration_Day
        ChartView.ChartType.WEEKLY -> R.string.CoinPage_TimeDuration_Week
        ChartView.ChartType.WEEKLY2 -> R.string.CoinPage_TimeDuration_TwoWeeks
        ChartView.ChartType.MONTHLY -> R.string.CoinPage_TimeDuration_Month
        ChartView.ChartType.MONTHLY_BY_DAY -> R.string.CoinPage_TimeDuration_Month
        ChartView.ChartType.MONTHLY3 -> R.string.CoinPage_TimeDuration_Month3
        ChartView.ChartType.MONTHLY6 -> R.string.CoinPage_TimeDuration_HalfYear
        ChartView.ChartType.MONTHLY12 -> R.string.CoinPage_TimeDuration_Year
        ChartView.ChartType.MONTHLY24 -> R.string.CoinPage_TimeDuration_Year2
    }
