package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.views.inflate

class CoinChartAdapter(
    viewItem: MutableLiveData<ViewItemWrapper>,
    private val currency: Currency,
    private val chartViewType: ChartViewType,
    private val listener: Listener,
    viewLifecycleOwner: LifecycleOwner,
) : ListAdapter<CoinChartAdapter.ViewItemWrapper, ChartViewHolder>(diff) {

    init {
        viewItem.observe(viewLifecycleOwner) {
            submitList(listOf(it))
        }
    }

    interface Listener {
        fun onChartTouchDown()
        fun onChartTouchUp()
        fun onTabSelect(chartType: ChartView.ChartType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        return ChartViewHolder(
            inflate(parent, R.layout.view_holder_coin_chart, false),
            listener,
            currency,
            chartViewType
        )
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {}

    override fun onBindViewHolder(
        holder: ChartViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val item = getItem(position)
        val prev = payloads.lastOrNull() as? ViewItemWrapper

        if (prev == null) {
            holder.bind(item)
        } else {
            holder.bindUpdate(item, prev)
        }
    }

    companion object {
        private val diff = object : DiffUtil.ItemCallback<ViewItemWrapper>() {
            override fun areItemsTheSame(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper,
            ): Boolean = true

            override fun areContentsTheSame(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper,
            ): Boolean {
                return oldItem.data == newItem.data
                        && oldItem.showError == newItem.showError
                        && oldItem.showSpinner == newItem.showSpinner
            }

            override fun getChangePayload(
                oldItem: ViewItemWrapper,
                newItem: ViewItemWrapper,
            ): Any? {
                return oldItem
            }
        }
    }

    data class ViewItemWrapper(
        val data: ChartInfoData?,
        val showSpinner: Boolean = false,
        val showError: Boolean = false,
    )

    enum class ChartViewType {
        CoinChart, MarketMetricChart
    }

}

