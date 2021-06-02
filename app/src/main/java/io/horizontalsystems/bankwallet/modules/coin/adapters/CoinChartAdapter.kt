package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.ChartPointViewItem
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.extensions.createTextView
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_chart.*
import java.math.BigDecimal
import java.util.*

class CoinChartAdapter(
        viewModel: CoinViewModel,
        viewLifecycleOwner: LifecycleOwner,
        private val listener: Listener
) : ListAdapter<CoinChartAdapter.ViewItemWrapper, ChartViewHolder>(diff) {

    init {
        viewModel.chartInfoLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    private val currency = viewModel.currency

    interface Listener {
        fun onChartTouchDown()
        fun onChartTouchUp()
        fun onTabSelect(chartType: ChartView.ChartType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        return ChartViewHolder(inflate(parent, R.layout.view_holder_coin_chart, false), listener, currency)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<ViewItemWrapper>() {
            override fun areItemsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean = true

            override fun areContentsTheSame(oldItem: ViewItemWrapper, newItem: ViewItemWrapper): Boolean {
                return oldItem.data?.chartData?.startTimestamp == newItem.data?.chartData?.startTimestamp
                        && oldItem.data?.chartData?.endTimestamp == newItem.data?.chartData?.endTimestamp
                        && oldItem.showError == newItem.showError
                        && oldItem.showSpinner == newItem.showSpinner
            }
        }
    }

    data class ViewItemWrapper(
            val data: ChartInfoData?,
            val showSpinner: Boolean,
            val showError: Boolean
    )

}

class ChartViewHolder(override val containerView: View, private val listener: CoinChartAdapter.Listener, private val currency: Currency)
    : RecyclerView.ViewHolder(containerView), LayoutContainer, Chart.Listener, TabLayout.OnTabSelectedListener {

    init {
        chart.setListener(this)
        bindActions()
        pointInfoVolumeTitle.isInvisible = true
        pointInfoVolume.isInvisible = true
    }

    private var macdIsEnabled = false
    private var enabledIndicator: ChartIndicator? = null

    fun bind(item: CoinChartAdapter.ViewItemWrapper) {

        if (item.showError) {
            chart.showError(containerView.context.getString(R.string.CoinPage_NoData))
            chart.hideSpinner()
            return
        }

        if (item.showSpinner) {
            chart.showSpinner()
        } else {
            chart.hideSpinner()
        }

        item.data?.let { data ->
            containerView.post {
                chart.setData(data.chartData, data.chartType, data.maxValue, data.minValue)
            }

            val indexOf = actions.indexOfFirst { it.first == data.chartType }
            if (indexOf > -1) {
                tabLayout.removeOnTabSelectedListener(this)
                tabLayout.selectTab(tabLayout.getTabAt(indexOf))
                tabLayout.addOnTabSelectedListener(this)
            }

            updateIndicatorsState(data.chartType)
        }

    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        chart.showSpinner()
        listener.onTabSelect(actions[tab.position].first)
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTouchDown() {
        listener.onChartTouchDown()
        chartPointsInfo.isInvisible = false
        tabLayout.isInvisible = true
    }

    override fun onTouchUp() {
        listener.onChartTouchUp()
        chartPointsInfo.isInvisible = true
        tabLayout.isInvisible = false
    }

    override fun onTouchSelect(point: PointInfo) {
        val price = CurrencyValue(currency, point.value.toBigDecimal())

        if (macdIsEnabled) {
            setSelectedPoint(ChartPointViewItem(point.timestamp, price, null, point.macdInfo))
        } else {
            val volume = point.volume?.let { volume ->
                CurrencyValue(currency, volume.toBigDecimal())
            }
            setSelectedPoint(ChartPointViewItem(point.timestamp, price, volume, null))
        }

        HudHelper.vibrate(containerView.context)
    }

    private fun updateIndicatorsState(chartType: ChartView.ChartType) {
        val enabled = chartType != ChartView.ChartType.DAILY && chartType != ChartView.ChartType.TODAY

        enabledIndicator?.let {
            chart.setIndicator(it, enabled)
        }

        indicatorEMA.isEnabled = enabled
        indicatorMACD.isEnabled = enabled
        indicatorRSI.isEnabled = enabled
    }

    private fun bindActions() {
        actions.forEach { (_, textId) ->
            tabLayout.newTab()
                    .setCustomView(createTextView(containerView.context, R.style.TabComponent).apply {
                        id = android.R.id.text1
                    })
                    .setText(containerView.context.getString(textId))
                    .let {
                        tabLayout.addTab(it, false)
                    }
        }

        tabLayout.tabRippleColor = null
        tabLayout.setSelectedTabIndicator(null)
        tabLayout.addOnTabSelectedListener(this)

        indicatorEMA.setOnClickListener {
            onIndicatorChanged(ChartIndicator.Ema, indicatorEMA.isChecked)
        }

        indicatorMACD.setOnClickListener {
            onIndicatorChanged(ChartIndicator.Macd, indicatorMACD.isChecked)
        }

        indicatorRSI.setOnClickListener {
            onIndicatorChanged(ChartIndicator.Rsi, indicatorRSI.isChecked)
        }

    }

    private fun onIndicatorChanged(indicator: ChartIndicator, checked: Boolean) {
        enabledIndicator = if (checked) indicator else null

        chart.setIndicator(indicator, checked)

        if (checked) {
            uncheckOtherIndicators(indicator)
        }

        macdIsEnabled = indicator == ChartIndicator.Macd && checked
    }

    private fun uncheckOtherIndicators(indicator: ChartIndicator) {
        val indicatorsToUncheck = ChartIndicator.values().filter { it != indicator }
        indicatorsToUncheck.forEach {
            when (it) {
                ChartIndicator.Ema -> indicatorEMA.isChecked = false
                ChartIndicator.Macd -> indicatorMACD.isChecked = false
                ChartIndicator.Rsi -> indicatorRSI.isChecked = false
            }
        }
    }

    private fun setSelectedPoint(item: ChartPointViewItem) {
        pointInfoVolumeTitle.isInvisible = true
        pointInfoVolume.isInvisible = true

        macdHistogram.isInvisible = true
        macdSignal.isInvisible = true
        macdValue.isInvisible = true

        pointInfoDate.text = DateHelper.getDayAndTime(Date(item.date * 1000))
        pointInfoPrice.text = App.numberFormatter.formatFiat(item.price.value, item.price.currency.symbol, 2, 4)

        item.volume?.let {
            pointInfoVolumeTitle.isInvisible = false
            pointInfoVolume.isInvisible = false
            pointInfoVolume.text = formatFiatShortened(item.volume.value, item.volume.currency.symbol)
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

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = App.numberFormatter.shortenValue(value)
        return App.numberFormatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }

    private fun getHistogramColor(value: Float): Int? {
        val textColor = if (value > 0) R.color.green_d else R.color.red_d
        return containerView.context.getColor(textColor)
    }

    companion object {
        private val actions = listOf(
                Pair(ChartView.ChartType.TODAY, R.string.CoinPage_TimeDuration_Today),
                Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
                Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
                Pair(ChartView.ChartType.WEEKLY2, R.string.CoinPage_TimeDuration_TwoWeeks),
                Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month),
                Pair(ChartView.ChartType.MONTHLY3, R.string.CoinPage_TimeDuration_Month3),
                Pair(ChartView.ChartType.MONTHLY6, R.string.CoinPage_TimeDuration_HalfYear),
                Pair(ChartView.ChartType.MONTHLY12, R.string.CoinPage_TimeDuration_Year),
                Pair(ChartView.ChartType.MONTHLY24, R.string.CoinPage_TimeDuration_Year2)
        )
    }
}
