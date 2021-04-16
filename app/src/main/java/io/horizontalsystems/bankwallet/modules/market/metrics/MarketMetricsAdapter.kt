package io.horizontalsystems.bankwallet.modules.market.metrics

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_totals.*
import java.math.BigDecimal

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
        holder.bind(
                getItem(position),
                { viewModel.refresh() },
                { viewModel.onBtcDominanceClick() },
                { viewModel.on24VolumeClick() },
                { viewModel.onDefiCapClick() },
                { viewModel.onTvlInDefiClick() }
        )
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

data class MarketMetricsWrapper(val marketMetrics: MarketMetrics?, val loading: Boolean, val showSyncError: Boolean)

class MarketMetricsViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
            data: MarketMetricsWrapper?,
            onErrorClick: (() -> Unit),
            onBtcDominanceClick: (() -> Unit),
            on24VolumeClick: (() -> Unit),
            onDefiCapClick: (() -> Unit),
            onTvlInDefiClick: (() -> Unit)
    ) {
        val metrics = data?.marketMetrics
        val diff = metrics?.totalMarketCap?.diff
        diff?.let {
            diffCircle.post {
                diffCircle.animateHorizontal(it.toFloat() * 3)
            }
        }

        btcDominance.setMetricData(metrics?.btcDominance)
        volume24h.setMetricData(metrics?.volume24h)
        defiCap.setMetricData(metrics?.defiCap)
        defiTvl.setMetricData(metrics?.defiTvl)

        metrics?.let {
            btcDominance.setOnClickListener { onBtcDominanceClick() }
            volume24h.setOnClickListener { on24VolumeClick() }
            defiCap.setOnClickListener { onDefiCapClick() }
            defiTvl.setOnClickListener { onTvlInDefiClick() }
        }

        marketCapTitle.alpha = if (metrics == null) 0.5f else 1f
        marketCapValue.text = metrics?.totalMarketCap?.value

        setDiffPercentage(diff, diffPercentage)

        progressBar.isVisible = data?.loading == true
        marketCapValue.isVisible = (metrics != null)
        diffPercentage.isVisible = (metrics != null)

        error.isVisible = data?.showSyncError == true
        error.setOnClickListener {
            onErrorClick()
        }
    }

    private fun setDiffPercentage(diff: BigDecimal?, view: TextView) {
        diff ?: return

        val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
        view.text = App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")

        val color = if (diff >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        view.setTextColor(containerView.context.getColor(color))
    }
}
