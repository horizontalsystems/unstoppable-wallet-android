package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_add_coin.*
import kotlinx.android.synthetic.main.view_holder_coin.*
import java.math.BigDecimal

class BalanceCoinAdapter(
        private val listener: Listener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(position: Int)
        fun onReceiveClicked(position: Int)
        fun onItemClick(position: Int)
        fun onAddCoinClick()
        fun onClickChart(position: Int)
    }

    private var items: List<BalanceViewItem> = listOf()

    private val coinType = 1
    private val addCoinType = 2

    private var expandedViewPosition: Int? = null

    fun toggleViewHolder(position: Int) {
        expandedViewPosition?.let {
            notifyItemChanged(it, false)
        }

        if (expandedViewPosition != position) {
            notifyItemChanged(position, true)
        }

        expandedViewPosition = if (expandedViewPosition == position) null else position
    }

    fun setItems(items: List<BalanceViewItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) addCoinType else coinType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            addCoinType -> ViewHolderAddCoin(inflate(parent, R.layout.view_holder_add_coin))
            else -> ViewHolderCoin(inflate(parent, R.layout.view_holder_coin), listener)
        }
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder is ViewHolderAddCoin) {
            holder.manageCoins.setOnSingleClickListener { listener.onAddCoinClick() }
        }

        if (holder !is ViewHolderCoin) return

        if (payloads.isEmpty()) {
            holder.bind(items[position], expandedViewPosition == position)
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(items[position], expandedViewPosition == position)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderCoin(override val containerView: View, private val listener: BalanceCoinAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var syncing = false

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean) {
        syncing = false
        buttonPay.isEnabled = false
        buttonReceive.isEnabled = true
        imgSyncFailed.visibility = View.INVISIBLE
        iconProgress.visibility = View.INVISIBLE

        balanceViewItem.state.let { adapterState ->
            when (adapterState) {
                is AdapterState.NotReady -> {
                    syncing = true
                    iconProgress.visibility = View.VISIBLE
                    textProgress.text = containerView.context.getString(R.string.Balance_Syncing)
                    buttonReceive.isEnabled = false
                }
                is AdapterState.Syncing -> {
                    syncing = true
                    iconProgress.visibility = View.VISIBLE
                    iconProgress.setProgress(adapterState.progress.toFloat())

                    textSyncedUntil.text = adapterState.lastBlockDate?.let {
                        containerView.context.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(it, "MMM d.yyyy"))
                    }

                    textProgress.text = containerView.context.getString(R.string.Balance_Syncing_WithProgress, adapterState.progress.toString())
                }
                is AdapterState.Synced -> {
                    if (balanceViewItem.coinValue.value > BigDecimal.ZERO) {
                        buttonPay.isEnabled = true
                    }
                    coinIcon.visibility = View.VISIBLE
                }
                is AdapterState.NotSynced -> {
                    imgSyncFailed.visibility = View.VISIBLE
                    coinIcon.visibility = View.GONE
                }
            }
        }

        coinAmount.text = App.numberFormatter.format(balanceViewItem.coinValue)
        coinAmount.alpha = if (balanceViewItem.state is AdapterState.Synced) 1f else 0.3f

        coinIcon.bind(balanceViewItem.coin)
        textCoinName.text = balanceViewItem.coin.title

        exchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.marketInfoExpired) R.color.grey_50 else R.color.grey))
        exchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
            when {
                balanceViewItem.chartInfo != null -> rateString
                else -> containerView.context.getString(R.string.Balance_RatePerCoin, rateString, balanceViewItem.coin.code)
            }
        }

        buttonPay.setOnSingleClickListener {
            listener.onSendClicked(adapterPosition)
        }

        chartButton.setOnClickListener { listener.onClickChart(adapterPosition) }

        buttonReceive.setOnSingleClickListener {
            listener.onReceiveClicked(adapterPosition)
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }

        showChart(balanceViewItem, expanded)

        showFiatAmount(balanceViewItem, syncing && !expanded)
        updateSecondLineItemsVisibility(expanded)
    }

    fun bindPartial(balanceViewItem: BalanceViewItem, expanded: Boolean) {
        viewHolderRoot.isSelected = expanded

        showFiatAmount(balanceViewItem, syncing && !expanded)
        updateSecondLineItemsVisibility(expanded)

        if (expanded) {
            AnimationHelper.expand(buttonsWrapper)
        } else {
            AnimationHelper.collapse(buttonsWrapper)
        }

        showChart(balanceViewItem, expanded)
    }

    private fun updateSecondLineItemsVisibility(expanded: Boolean) {
        textProgress.visibility = if (syncing && !expanded) View.VISIBLE else View.GONE
        textSyncedUntil.visibility = if (syncing && !expanded) View.VISIBLE else View.GONE
        coinAmount.visibility = if (syncing && !expanded) View.GONE else View.VISIBLE
    }

    private fun showFiatAmount(balanceViewItem: BalanceViewItem, collapsedAndSyncing: Boolean) {
        balanceViewItem.currencyValue?.let {
            fiatAmount.text = App.numberFormatter.format(it, trimmable = true)
            fiatAmount.alpha = if (!balanceViewItem.marketInfoExpired && balanceViewItem.state is AdapterState.Synced) 1f else 0.5f
        }

        fiatAmount.visibility = when {
            collapsedAndSyncing -> View.GONE
            balanceViewItem.currencyValue == null -> View.GONE
            balanceViewItem.currencyValue.value.compareTo(BigDecimal.ZERO) == 0 -> View.GONE
            else -> View.VISIBLE
        }
    }

    private fun showChart(viewItem: BalanceViewItem, expanded: Boolean) {
        if (expanded || viewItem.chartInfo == null) {
            return setChartVisibility(false)
        }

        setChartVisibility(true)

        chartLoading.visibility = View.INVISIBLE
        textChartError.visibility = View.INVISIBLE
        chartView.visibility = View.INVISIBLE

        viewItem.diff?.let {
            txDiff.bind(it, containerView.context)
            txDiff.visibility = View.VISIBLE
        } ?: run {
            txDiff.visibility = View.GONE
        }

        val chartInfo = viewItem.chartInfo

        chartView.visibility = View.VISIBLE
        chartView.setData(chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }, ChartType.DAILY, chartInfo.startTimestamp, chartInfo.endTimestamp)
    }

    private fun setChartVisibility(show: Boolean) {
        if (show) {
            chartButton.visibility = View.VISIBLE
            chartViewWrapper.visibility = View.VISIBLE
            txDiff.visibility = View.VISIBLE
        } else {
            chartButton.visibility = View.INVISIBLE
            chartViewWrapper.visibility = View.GONE
            txDiff.visibility = View.INVISIBLE
        }
    }
}
