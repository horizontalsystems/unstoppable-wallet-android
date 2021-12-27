package io.horizontalsystems.bankwallet.modules.chart

import android.util.Range
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
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.*

open class ChartViewModel(private val service: AbstractChartService) : ViewModel() {
    val tabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val indicatorsLiveData = MutableLiveData<List<TabItem<ChartIndicator>>>()
    val dataWrapperLiveData = MutableLiveData<ChartDataWrapper>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val viewStateLiveData = MutableLiveData<ViewState>()

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

        Observable
            .combineLatest(
                service.indicatorObservable,
                service.indicatorsEnabledObservable,
                { selectedIndicator, enabled ->
                    Pair(selectedIndicator, enabled)
                }
            )
            .subscribeIO { (selectedIndicator, enabled) ->
                val indicators = service.chartIndicators.map { indicator ->
                    TabItem(Translator.getString(indicator.stringResId),
                        indicator == selectedIndicator.orElse(null),
                        indicator,
                        enabled = enabled)
                }
                indicatorsLiveData.postValue(indicators)
            }
            .let {
                disposables.add(it)
            }

        service.chartPointsWrapperObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.viewState?.let {
                    viewStateLiveData.postValue(it)
                }

                loadingLiveData.postValue(chartItemsDataState.loading)

                chartItemsDataState.dataOrNull?.let {
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

    private fun syncChartItems(chartPointsWrapper: ChartPointsWrapper) {
        val chartItems = chartPointsWrapper.items
        if (chartItems.isEmpty()) return

        val lastItemValue = chartItems.last().value
        val currentValue = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue.toBigDecimal()))

        val firstItemValue = chartItems.first().value
        val currentValueDiff = Value.Percent(((lastItemValue - firstItemValue) / firstItemValue * 100).toBigDecimal())

        val chartData = ChartDataBuilder.buildFromPoints(
            chartPointsWrapper.items,
            chartPointsWrapper.startTimestamp,
            chartPointsWrapper.endTimestamp,
            chartPointsWrapper.isExpired)

        val (minValue, maxValue) = getMinMax(chartData.valueRange)

        val chartInfoData = ChartInfoData(
            chartData,
            chartPointsWrapper.chartType,
            maxValue,
            minValue
        )

        dataWrapperLiveData.postValue(ChartDataWrapper(currentValue, currentValueDiff, chartInfoData))
    }

    private val noChangesLimitPercent = 0.2f
    private fun getMinMax(range: Range<Float>): Pair<String?, String?> {
        var max = range.upper
        var min = range.lower

        if (max!= null && min != null && max == min){
            min *= (1 - noChangesLimitPercent)
            max *= (1 + noChangesLimitPercent)
        }

        val maxValue = max?.let { getFormattedValue(it, service.currency) }
        val minValue = min?.let { getFormattedValue(it, service.currency) }

        return Pair(minValue, maxValue)

    }

    private fun getFormattedValue(value: Float, currency: Currency): String {
        return App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(currency,  value.toBigDecimal()))
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun getSelectedPoint(item: ChartDataItemImmutable): SelectedPoint? {
        return item.values[Indicator.Candle]?.let { candle ->
            val value = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, candle.value.toBigDecimal()))
            val dayAndTime = DateHelper.getDayAndTime(Date(item.timestamp * 1000))

            val extraData = when (service.indicator) {
                ChartIndicator.Macd -> {
                    val macd = item.values[Indicator.Macd]?.let {
                        App.numberFormatter.format(it.value, 0, 2)
                    }
                    val histogram = item.values[Indicator.MacdHistogram]?.let {
                        App.numberFormatter.format(it.value, 0, 2)
                    }
                    val signal = item.values[Indicator.MacdSignal]?.let {
                        App.numberFormatter.format(it.value, 0, 2)
                    }

                    SelectedPoint.ExtraData.Macd(macd, histogram, signal)
                }
                else -> {
                    item.values[Indicator.Volume]?.let { volume ->
                        SelectedPoint.ExtraData.Volume(
                            App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, volume.value.toBigDecimal()))
                        )
                    }
                }
            }

            SelectedPoint(
                value = value,
                date = dayAndTime,
                extraData = extraData,
            )
        }
    }

}

data class SelectedPoint(
    val value: String,
    val date: String,
    val extraData: ExtraData?
) {
    sealed class ExtraData {
        class Volume(val volume: String) : ExtraData()
        class Macd(val macd: String?, val histogram: String?, val signal: String?) : ExtraData()
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
