package io.horizontalsystems.bankwallet.modules.market.metrics

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_totals.*

class MarketMetricsAdapter(private val viewModel: MarketMetricsViewModel, viewLifecycleOwner: LifecycleOwner)
    : ListAdapter<MarketMetricsWrapper, MarketMetricsViewHolder>(diff) {

    init {
        viewModel.marketMetricsLiveData.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketMetricsViewHolder {
        return MarketMetricsViewHolder(inflate(parent, R.layout.view_holder_market_totals, false))
    }

    override fun onBindViewHolder(holder: MarketMetricsViewHolder, position: Int) = Unit

    override fun onBindViewHolder(holder: MarketMetricsViewHolder, position: Int, payloads: MutableList<Any>) {
        holder.bind(getItem(position)) {
            viewModel.refresh()
        }
    }

    fun refresh() {
        viewModel.refresh()
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<MarketMetricsWrapper>() {
            override fun areItemsTheSame(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Boolean = true
            override fun areContentsTheSame(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Boolean = false
            override fun getChangePayload(oldItem: MarketMetricsWrapper, newItem: MarketMetricsWrapper): Any = oldItem
        }
    }
}

data class MarketMetricsWrapper(val marketMetrics: MarketMetrics?, val loading: Boolean, val error: String? = null)

class MarketMetricsViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(data: MarketMetricsWrapper?, onErrorClick: (() -> Unit)) {
        val metrics = data?.marketMetrics

        totalMarketCap.setMetricData(metrics?.totalMarketCap)
        btcDominance.setMetricData(metrics?.btcDominance)
        volume24h.setMetricData(metrics?.volume24h)
        defiCap.setMetricData(metrics?.defiCap)
        defiTvl.setMetricData(metrics?.defiTvl)

        progressBar.isVisible = data?.loading == true

        error.isVisible = data?.error != null
        error.text = data?.error
        error.setOnClickListener {
            onErrorClick()
        }
    }
}

