package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
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

val ChartType.viewChartType: ChartView.ChartType
    get() = when (this) {
        ChartType.TODAY -> ChartView.ChartType.TODAY
        ChartType.DAILY -> ChartView.ChartType.DAILY
        ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
        ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
        ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
        ChartType.MONTHLY_BY_DAY -> ChartView.ChartType.MONTHLY_BY_DAY
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
        ChartView.ChartType.MONTHLY -> ChartType.MONTHLY
        ChartView.ChartType.MONTHLY_BY_DAY -> ChartType.MONTHLY_BY_DAY
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
    val description by service::description
    val poweredBy by service::poweredBy
    val currency by service::currency
    val chartInfoLiveData = MutableLiveData<ChartInfoData>()
    val viewStateLiveData = MutableLiveData<ViewState>()
    val chartLoadingLiveData = MutableLiveData<Boolean>()
    val chartTabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val currentValueLiveData = MutableLiveData<String>()
    val currentValueDiffLiveData = MutableLiveData<Value.Percent>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO {
                syncChartItems(it)
            }
            .let {
                disposables.add(it)
            }

        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartTypes.map {
                    TabItem(Translator.getString(it.stringResId), it == chartType, it.viewChartType)
                }
                chartTabItemsLiveData.postValue(tabItems)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    override fun onCleared() {
        service.stop()
    }

    private fun syncChartItems(chartData: DataState<Pair<ChartType, List<MetricChartModule.Item>>>) {
        chartData.viewState?.let {
            viewStateLiveData.postValue(it)
        }

        chartLoadingLiveData.postValue(chartData.loading)

        chartData.dataOrNull?.let { (chartType, chartItems) ->
            chartItems.lastOrNull()?.let { lastItem ->
                val lastItemValue = lastItem.value
                currentValueLiveData.postValue(
                    App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))
                )

                val firstItemValue = chartItems.first().value
                currentValueDiffLiveData.postValue(Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal()))
            }

            val chartViewItem = factory.convert(
                chartItems,
                chartType.viewChartType,
                MetricChartModule.ValueType.CompactCurrencyValue,
                service.currency
            )

            val data = ChartInfoData(
                chartViewItem.chartData,
                chartViewItem.chartType,
                chartViewItem.maxValue,
                chartViewItem.minValue
            )
            chartInfoLiveData.postValue(data)
        }
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType.kitChartType)
    }


}
