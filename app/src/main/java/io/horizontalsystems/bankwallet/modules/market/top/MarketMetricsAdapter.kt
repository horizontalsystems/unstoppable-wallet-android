package io.horizontalsystems.bankwallet.modules.market.top

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_totals.*

class MarketMetricsAdapter : ListAdapter<MarketMetricsWrapper, MarketMetricsViewHolder>(diff) {

    init {
        submitList(listOf(MarketMetricsWrapper(null)))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketMetricsViewHolder {
        return MarketMetricsViewHolder(inflate(parent, R.layout.view_holder_market_totals, false))
    }

    override fun onBindViewHolder(holder: MarketMetricsViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: MarketMetricsViewHolder, position: Int, payloads: MutableList<Any>) {
        holder.bind(getItem(position).marketMetrics, (payloads.firstOrNull() as? MarketMetricsWrapper)?.marketMetrics)
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MarketMetricsWrapper>() {
            override fun areItemsTheSame(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Boolean = true
            override fun areContentsTheSame(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Boolean = false
            override fun getChangePayload(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Any = oldItem
        }
    }
}

data class MarketMetricsWrapper(val marketMetrics: MarketMetrics?)

class MarketMetricsViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(current: MarketMetrics?, prev: MarketMetrics?) {

        totalMarketCap.setMetricData(current?.totalMarketCap)
        btcDominance.setMetricData(current?.btcDominance)
        volume24h.setMetricData(current?.volume24h)
        defiCap.setMetricData(current?.defiCap)
        defiTvl.setMetricData(current?.defiTvl)
    }

}

