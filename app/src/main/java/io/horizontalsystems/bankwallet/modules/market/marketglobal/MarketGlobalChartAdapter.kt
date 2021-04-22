package io.horizontalsystems.bankwallet.modules.market.marketglobal

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.ChartPointViewItem
import io.horizontalsystems.bankwallet.ui.extensions.createTextView
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_metrics_global_chart.*
import java.util.*

class MarketGlobalChartAdapter(private val listener: Listener, private val chartType: ChartView.ChartType)
    : RecyclerView.Adapter<MarketGlobalChartAdapter.ChartViewHolder>() {

    interface Listener {
        fun onChartTouchDown()
        fun onChartTouchUp()
        fun onTabSelected(chartType: ChartView.ChartType)
    }

    override fun getItemCount() = 1
    private var chartViewItem: ChartViewItem? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        return ChartViewHolder.create(parent, listener, chartType)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            chartViewItem?.let { holder.bind(it) }
        } else {
            payloads.firstOrNull().let { payload ->
                when (payload) {
                    is ChartViewItem -> holder.bind(payload)
                }
            }
        }
    }

    fun setChartViewItem(chartViewItem: ChartViewItem) {
        this.chartViewItem = chartViewItem
        notifyItemChanged(0, chartViewItem)
    }

    class ChartViewHolder(
            override val containerView: View,
            private val listener: Listener,
            private var chartType: ChartView.ChartType
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer, Chart.Listener, TabLayout.OnTabSelectedListener {

        private val actions = listOf(
                Pair(ChartView.ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
                Pair(ChartView.ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
                Pair(ChartView.ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month),
        )

        private var currency: Currency? = null

        init {
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

            chart.setListener(this)

            setChartType(chartType)
        }

        fun bind(chartViewItem: ChartViewItem) {

            topValue.text = chartViewItem.lastValueWithDiff?.value
            diffValue.setDiff(chartViewItem.lastValueWithDiff?.diff)

            if (chartViewItem.loading) {
                chart.showSinner()
            } else {
                chart.hideSinner()

                chartViewItem.chartData?.let {
                    chart.showChart()
                    containerView.post {
                        chart.setData(it, chartViewItem.chartType, chartViewItem.maxValue, chartViewItem.minValue)
                    }
                }
            }

            currency = chartViewItem.currency
        }

        //Chart.Listener

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
            val currency = currency ?: return
            val price = CurrencyValue(currency, point.value.toBigDecimal())
            val item = ChartPointViewItem(point.timestamp, price, null, null)

            pointInfoDate.text = DateHelper.getDayAndTime(Date(item.date * 1000))
            pointInfoPrice.text = App.numberFormatter.formatFiat(item.price.value, item.price.currency.symbol, 2, 4)
        }

        // TabLayout.OnTabSelectedListener

        override fun onTabSelected(tab: TabLayout.Tab) {
            chartType = actions[tab.position].first
            listener.onTabSelected(chartType)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
        }

        private fun setChartType(type: ChartView.ChartType) {
            val indexOf = actions.indexOfFirst { it.first == type }
            if (indexOf > -1) {
                tabLayout.removeOnTabSelectedListener(this)
                tabLayout.selectTab(tabLayout.getTabAt(indexOf))
                tabLayout.addOnTabSelectedListener(this)
            }
        }

        companion object {
            const val layout = R.layout.view_holder_metrics_global_chart

            fun create(parent: ViewGroup, listener: Listener, chartType: ChartView.ChartType) = ChartViewHolder(inflate(parent, layout, false), listener, chartType)
        }

    }
}
