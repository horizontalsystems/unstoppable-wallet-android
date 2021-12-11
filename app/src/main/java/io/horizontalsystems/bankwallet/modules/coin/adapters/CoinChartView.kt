package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.ChartPointViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.TabButtonSecondaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.TabPeriod
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.coin_chart.view.*
import java.math.BigDecimal
import java.util.*

class CoinChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr), Chart.Listener {

    private var chartTypes: List<Pair<ChartView.ChartType, Int>> = listOf()
    private lateinit var currency: Currency
    private lateinit var listener: CoinChartAdapter.Listener
    private lateinit var chartViewType: CoinChartAdapter.ChartViewType

    fun setListener(listener: CoinChartAdapter.Listener) {
        this.listener = listener
    }

    fun setCurrency(currency: Currency) {
        this.currency = currency
    }

    fun setChartViewType(chartViewType: CoinChartAdapter.ChartViewType) {
        this.chartViewType = chartViewType
    }

    init {
        inflate(context, R.layout.coin_chart, this)

        chart.setListener(this)
        pointTitle.isInvisible = true
        pointValue.isInvisible = true
    }

    private var macdIsEnabled = false
    private var enabledIndicator: ChartIndicator? = null
    private var item: CoinChartAdapter.ViewItemWrapper? = null

    fun bindNew(item: CoinChartAdapter.ViewItemWrapper) {
        this.item?.let { prevItem ->
            bindUpdate(item, prevItem)
        } ?: bind(item)
    }

    fun bind(item: CoinChartAdapter.ViewItemWrapper) {
        this.item = item

        if (item.showError) {
            chart.showError(context.getString(R.string.CoinPage_NoData))
            chart.hideSpinner()
        }

        if (item.showSpinner) {
            chart.showSpinner()
        } else {
            chart.hideSpinner()
        }

        item.data?.let { data ->
            post {
                chart.setData(data.chartData, data.chartType, data.maxValue, data.minValue)
            }

            bindTabs(getSelectedTabIndex(data.chartType))

            updateIndicatorsState(data.chartType)
        }

    }

    private fun getSelectedTabIndex(chartType: ChartView.ChartType): Int {
        var selectedIndex = chartTypes.indexOfFirst { it.first == chartType }
        if (selectedIndex < 0) {
            selectedIndex = 0
        }
        return selectedIndex
    }

    fun bindUpdate(
        current: CoinChartAdapter.ViewItemWrapper,
        prev: CoinChartAdapter.ViewItemWrapper,
    ) {
        this.item = current

        current.apply {
            if (showSpinner != prev.showSpinner) {
                if (showSpinner) {
                    chart.showSpinner()
                } else {
                    chart.hideSpinner()
                }
            }
            if (showError != prev.showError && showError) {
                chart.showError(context.getString(R.string.CoinPage_NoData))
                chart.hideSpinner()
            }
            if (data != prev.data) {
                data?.let { data ->
                    chart.setData(data.chartData, data.chartType, data.maxValue, data.minValue)

                    bindTabs(getSelectedTabIndex(data.chartType))

                    updateIndicatorsState(data.chartType)
                }
            }
        }
    }

    override fun onTouchDown() {
        listener.onChartTouchDown()
        chartPointsInfo.isInvisible = false
        tabCompose.isInvisible = true
    }

    override fun onTouchUp() {
        listener.onChartTouchUp()
        chartPointsInfo.isInvisible = true
        tabCompose.isInvisible = false
    }

    override fun onTouchSelect(point: PointInfo) {
        val price = CurrencyValue(currency, point.value.toBigDecimal())

        if (macdIsEnabled) {
            setSelectedPoint(ChartPointViewItem(point.timestamp, price, null, null, point.macdInfo))
        } else {
            val volume = point.volume?.let { volume ->
                CurrencyValue(currency, volume.toBigDecimal())
            }
            setSelectedPoint(ChartPointViewItem(point.timestamp, price, volume, point.dominance, null))
        }

        HudHelper.vibrate(context)
    }


    private fun updateIndicatorsState(chartType: ChartView.ChartType) {
        val enabled =
            chartType != ChartView.ChartType.DAILY && chartType != ChartView.ChartType.TODAY

        enabledIndicator?.let {
            chart.setIndicator(it, enabled)
        }

        setIndicators(enabled)
    }

    private fun bindTabs(selectedIndex: Int = 0) {
        tabCompose.setContent {
            ComposeAppTheme {
                CustomTab(chartTypes, selectedIndex)
            }
        }
    }

    @Composable
    private fun CustomTab(tabs: List<Pair<ChartView.ChartType, Int>>, selectedIndex: Int) {
        var tabIndex by remember { mutableStateOf(selectedIndex) }

        TabPeriod {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = Color.Transparent,
                edgePadding = 0.dp,
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, (chartType, titleResId) ->
                    val selected = tabIndex == index

                    Tab(
                        selected = selected,
                        onClick = { },
                    ) {
                        TabButtonSecondaryTransparent(
                            title = stringResource(id = titleResId),
                            onSelect = {
                                tabIndex = index
                                listener.onTabSelect(chartType)
                            },
                            selected = selected
                        )
                    }
                }
            }
        }
    }

    private fun setIndicators(enabled: Boolean) {
        if (chartViewType == CoinChartAdapter.ChartViewType.MarketMetricChart) {
            indicatorsCompose.isVisible = false
            return
        }

        indicatorsCompose.setContent {
            ComposeAppTheme {
                Column {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(top = 7.dp)
                    ) {
                        items(getIndicators(enabled)) { item ->
                            TabButtonSecondary(
                                title = getIndicatorTitle(item.indicator),
                                onSelect = {
                                    onIndicatorChanged(item.indicator, !item.checked)
                                    setIndicators(enabled)
                                },
                                selected = item.checked,
                                enabled = item.enabled
                            )
                        }
                    }
                    Divider(
                        modifier = Modifier.padding(top = 7.dp),
                        thickness = 1.dp,
                        color = ComposeAppTheme.colors.steel10
                    )
                }
            }
        }
    }

    private fun getIndicators(enabled: Boolean): List<IndicatorViewItem> {
        return listOf(
            IndicatorViewItem(ChartIndicator.Ema, enabledIndicator == ChartIndicator.Ema, enabled),
            IndicatorViewItem(ChartIndicator.Macd,
                enabledIndicator == ChartIndicator.Macd,
                enabled),
            IndicatorViewItem(ChartIndicator.Rsi, enabledIndicator == ChartIndicator.Rsi, enabled)
        )
    }

    private fun getIndicatorTitle(indicator: ChartIndicator): String {
        return when (indicator) {
            ChartIndicator.Ema -> context.getString(R.string.CoinPage_IndicatorEMA)
            ChartIndicator.Macd -> context.getString(R.string.CoinPage_IndicatorMACD)
            ChartIndicator.Rsi -> context.getString(R.string.CoinPage_IndicatorRSI)
        }
    }

    private fun onIndicatorChanged(indicator: ChartIndicator, checked: Boolean) {
        enabledIndicator = if (checked) indicator else null

        chart.setIndicator(indicator, checked)

        macdIsEnabled = indicator == ChartIndicator.Macd && checked
    }

    private fun setSelectedPoint(item: ChartPointViewItem) {
        pointTitle.isInvisible = true
        pointValue.isInvisible = true

        macdHistogram.isInvisible = true
        macdSignal.isInvisible = true
        macdValue.isInvisible = true

        pointInfoDate.text = DateHelper.getDayAndTime(Date(item.date * 1000))
        pointInfoPrice.text =
            getFormattedPointValue(item.price.value, item.price.currency.symbol, chartViewType)

        item.volume?.let {
            pointTitle.isInvisible = false
            pointValue.isInvisible = false
            pointTitle.text = context.getString(R.string.CoinPage_Volume)
            pointValue.setTextColor(context.getColor(R.color.grey))
            pointValue.text =
                formatFiatShortened(item.volume.value, item.volume.currency.symbol)
        }

        item.dominance?.let {
            pointTitle.isInvisible = false
            pointValue.isInvisible = false
            pointTitle.text = context.getString(R.string.CoinPage_IndicatorDominace)
            pointValue.setTextColor(context.getColor(R.color.jacob))
            pointValue.text = App.numberFormatter.format(it, 0, 2, suffix = "%")
        }

        item.macdInfo?.let { macdInfo ->
            macdInfo.histogram?.let {
                macdHistogram.isVisible = true
                getHistogramColor(it)?.let { it1 -> macdHistogram.setTextColor(it1) }
                macdHistogram.text = App.numberFormatter.format(it, 0, 2)
            }
            macdInfo.signal?.let {
                macdSignal.isVisible = true
                macdSignal.text = App.numberFormatter.format(it, 0, 2)
            }
            macdInfo.macd?.let {
                macdValue.isVisible = true
                macdValue.text = App.numberFormatter.format(it, 0, 2)
            }
        }
    }

    private fun getFormattedPointValue(
        value: BigDecimal, symbol: String,
        chartViewType: CoinChartAdapter.ChartViewType,
    ): String {
        return when (chartViewType) {
            CoinChartAdapter.ChartViewType.CoinChart -> {
                App.numberFormatter.formatFiat(value, symbol, 2, 4)
            }
            CoinChartAdapter.ChartViewType.MarketMetricChart -> {
                val (shortenValue, suffix) = App.numberFormatter.shortenValue(value)
                App.numberFormatter.formatFiat(shortenValue, symbol, 0, 2) + " $suffix"
            }
        }
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortCapValue.first,
            symbol,
            0,
            2) + " " + shortCapValue.second
    }

    private fun getHistogramColor(value: Float): Int {
        val textColor = if (value > 0) R.color.green_d else R.color.red_d
        return context.getColor(textColor)
    }

    fun setChartTypes(chartTypes: List<Pair<ChartView.ChartType, Int>>) {
        this.chartTypes = chartTypes
    }

    data class IndicatorViewItem(
        val indicator: ChartIndicator,
        val checked: Boolean,
        val enabled: Boolean,
    )
}
