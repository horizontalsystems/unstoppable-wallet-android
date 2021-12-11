package io.horizontalsystems.bankwallet.modules.coin.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.core.entities.Currency
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_coin_chart.*

class ChartViewHolder(
    override val containerView: View,
    listener: CoinChartAdapter.Listener,
    currency: Currency,
    chartViewType: CoinChartAdapter.ChartViewType,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        coinChartView.setListener(listener)
        coinChartView.setCurrency(currency)
        coinChartView.setChartViewType(chartViewType)
    }

    fun bind(item: CoinChartAdapter.ViewItemWrapper) {
        coinChartView.bind(item)
    }

    fun bindUpdate(
        item: CoinChartAdapter.ViewItemWrapper,
        prev: CoinChartAdapter.ViewItemWrapper,
    ) {
        coinChartView.bindUpdate(item, prev)
    }
}
