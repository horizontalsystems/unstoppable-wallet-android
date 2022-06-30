package io.horizontalsystems.bankwallet.modules.chart

import android.util.Range
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.viewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartDataBuilder
import io.horizontalsystems.chartview.ChartDataItemImmutable
import io.horizontalsystems.chartview.Indicator
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.*

open class ChartViewModel(
    private val service: AbstractChartService,
    private val valueFormatter: ChartModule.ChartNumberFormatter
) : ViewModel() {
    val tabItemsLiveData = MutableLiveData<List<TabItem<HsTimePeriod>>>()
    val indicatorsLiveData = MutableLiveData<List<TabItem<ChartIndicator>>>()
    val dataWrapperLiveData = MutableLiveData<ChartDataWrapper>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)

    private val disposables = CompositeDisposable()

    init {
        loadingLiveData.postValue(true)

        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartIntervals.map {
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

                loadingLiveData.postValue(false)

                chartItemsDataState.getOrNull()?.let {
                    syncChartItems(it)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun onSelectChartInterval(chartInterval: HsTimePeriod) {
        loadingLiveData.postValue(true)
        service.updateChartInterval(chartInterval)
    }

    fun onSelectIndicator(chartIndicator: ChartIndicator?) {
        loadingLiveData.postValue(true)
        service.updateIndicator(chartIndicator)
    }

    fun refresh() {
        loadingLiveData.postValue(true)
        service.refresh()
    }

    private fun syncChartItems(chartPointsWrapper: ChartPointsWrapper) {
        val chartItems = chartPointsWrapper.items
        if (chartItems.isEmpty()) return

        val lastItemValue = chartItems.last().value
        val currentValue = valueFormatter.formatValue(service.currency, lastItemValue.toBigDecimal())

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
            chartPointsWrapper.chartInterval,
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
        return valueFormatter.formatValue(currency,  value.toBigDecimal())
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    fun getSelectedPoint(item: ChartDataItemImmutable): SelectedPoint? {
        return item.values[Indicator.Candle]?.let { candle ->
            val value = valueFormatter.formatValue(service.currency, candle.value.toBigDecimal())
            val dayAndTime = DateHelper.getDayAndTime(Date(item.timestamp * 1000))

            SelectedPoint(
                value = value,
                date = dayAndTime,
                extraData = getItemExtraData(item),
            )
        }
    }

    private fun getItemExtraData(item: ChartDataItemImmutable): SelectedPoint.ExtraData? {
        if (service.indicator == ChartIndicator.Macd) {
            val macd = item.values[Indicator.Macd]?.let {
                App.numberFormatter.format(it.value, 0, 2)
            }
            val histogram = item.values[Indicator.MacdHistogram]?.let {
                App.numberFormatter.format(it.value, 0, 2)
            }
            val signal = item.values[Indicator.MacdSignal]?.let {
                App.numberFormatter.format(it.value, 0, 2)
            }

            return SelectedPoint.ExtraData.Macd(macd, histogram, signal)
        }

        val dominance = item.values[Indicator.Dominance]
        val volume = item.values[Indicator.Volume]

        return when {
            dominance != null -> SelectedPoint.ExtraData.Dominance(
                App.numberFormatter.format(dominance.value, 0, 2, suffix = "%")
            )
            volume != null -> SelectedPoint.ExtraData.Volume(
                App.numberFormatter.formatFiatShort(volume.value.toBigDecimal(), service.currency.symbol, 2)
            )
            else -> null
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
        class Dominance(val dominance: String) : ExtraData()
        class Macd(val macd: String?, val histogram: String?, val signal: String?) : ExtraData()
    }
}

private val ChartIndicator.stringResId: Int
    get() = when (this) {
        ChartIndicator.Ema -> R.string.CoinPage_IndicatorEMA
        ChartIndicator.Macd -> R.string.CoinPage_IndicatorMACD
        ChartIndicator.Rsi -> R.string.CoinPage_IndicatorRSI
    }

val HsTimePeriod.stringResId: Int
    get() = when (this) {
        HsTimePeriod.Day1 -> R.string.CoinPage_TimeDuration_Day
        HsTimePeriod.Week1 -> R.string.CoinPage_TimeDuration_Week
        HsTimePeriod.Week2 -> R.string.CoinPage_TimeDuration_TwoWeeks
        HsTimePeriod.Month1 -> R.string.CoinPage_TimeDuration_Month
        HsTimePeriod.Month3 -> R.string.CoinPage_TimeDuration_Month3
        HsTimePeriod.Month6 -> R.string.CoinPage_TimeDuration_HalfYear
        HsTimePeriod.Year1 -> R.string.CoinPage_TimeDuration_Year
    }
