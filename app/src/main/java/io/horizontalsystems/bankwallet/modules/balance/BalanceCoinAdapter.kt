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
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
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
        fun onSendClicked(viewItem: BalanceViewItem)
        fun onReceiveClicked(viewItem: BalanceViewItem)
        fun onChartClicked(viewItem: BalanceViewItem)
        fun onItemClicked(viewItem: BalanceViewItem)
        fun onAddCoinClicked()
    }

    private var items: List<BalanceViewItem> = listOf()

    private val coinType = 1
    private val addCoinType = 2

    private var expandedViewPosition: Int? = null

    fun toggleViewHolder(viewItem: BalanceViewItem) {
        val position= items.indexOf(viewItem)
        if (position == -1)
            return

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
            else -> ViewHolderCoin(inflate(parent, R.layout.view_holder_coin))
        }
    }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder is ViewHolderAddCoin) {
            holder.manageCoins.setOnSingleClickListener { listener.onAddCoinClicked() }
        }

        if (holder !is ViewHolderCoin) return

        if (payloads.isEmpty()) {
            val item = items[position]
            holder.bind(item, expandedViewPosition == position, ViewHolderCoinListener(item, listener))
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(items[position], expandedViewPosition == position)
        }
    }

    // ViewHolderCoin.Listener

    private class ViewHolderCoinListener(
            private val item: BalanceViewItem,
            private val adapterListener: Listener
    ) : ViewHolderCoin.Listener {

        override fun onSendClicked() {
            adapterListener.onSendClicked(item)
        }

        override fun onReceiveClicked() {
            adapterListener.onReceiveClicked(item)
        }

        override fun onChartClicked() {
            adapterListener.onChartClicked(item)
        }

        override fun onItemClicked() {
            adapterListener.onItemClicked(item)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var syncing = false

    interface Listener {
        fun onSendClicked()
        fun onReceiveClicked()
        fun onChartClicked()
        fun onItemClicked()
    }

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean, listener: Listener) {
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

        showLockedBalance(balanceViewItem)

        coinIcon.bind(balanceViewItem.coin)
        textCoinName.text = balanceViewItem.coin.title

        exchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.marketInfoExpired) R.color.grey_50 else R.color.grey))
        exchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
            when {
                balanceViewItem.chartInfoState is ChartInfoState.Loaded -> rateString
                else -> containerView.context.getString(R.string.Balance_RatePerCoin, rateString, balanceViewItem.coin.code)
            }
        }

        buttonPay.setOnSingleClickListener {
            listener.onSendClicked()
        }

        chartButton.setOnClickListener { listener.onChartClicked() }

        buttonReceive.setOnSingleClickListener {
            listener.onReceiveClicked()
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            listener.onItemClicked()
        }

        showChart(balanceViewItem, expanded)

        showFiatAmount(balanceViewItem, syncing && !expanded)
        updateSecondLineItemsVisibility(expanded)
    }

    private fun showLockedBalance(balanceViewItem: BalanceViewItem) {
        coinAmountLocked.visibility = View.GONE
        fiatAmountLocked.visibility = View.GONE

        if (balanceViewItem.coinValueLocked.value > BigDecimal.ZERO) {
            coinAmountLocked.visibility = View.VISIBLE
            coinAmountLocked.text = App.numberFormatter.format(balanceViewItem.coinValueLocked)

            balanceViewItem.currencyValueLocked?.let {
                fiatAmountLocked.visibility = View.VISIBLE

                fiatAmountLocked.text = App.numberFormatter.format(it, trimmable = true)
                fiatAmountLocked.alpha = if (!balanceViewItem.marketInfoExpired && balanceViewItem.state is AdapterState.Synced) 1f else 0.5f
            }
        }
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
        if (expanded || viewItem.chartInfoState !is ChartInfoState.Loaded) {
            return setChartVisibility(false)
        }

        setChartVisibility(true)

        chartLoading.visibility = View.INVISIBLE
        textChartError.visibility = View.INVISIBLE
        chartView.visibility = View.INVISIBLE

        viewItem.diff?.let {
            txDiff.bind(it, containerView.context, false)
            txDiff.visibility = View.VISIBLE
        } ?: run {
            txDiff.visibility = View.GONE
        }

        val chartInfo = viewItem.chartInfoState.chartInfo

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
