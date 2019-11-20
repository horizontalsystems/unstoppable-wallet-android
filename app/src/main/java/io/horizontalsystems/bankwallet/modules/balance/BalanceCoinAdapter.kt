package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.ChartInfoState
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
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
        // Update with regular method for the initial load to avoid showing balance tab with empty list
        if (this.items.isEmpty()) {
            this.items = items
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(BalanceViewItemDiff(this.items, items))
            this.items = items
            diffResult.dispatchUpdatesTo(this)
        }
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
            holder.manageCoins.setOnSingleClickListener { listener.onAddCoinClicked() }
        }

        if (holder !is ViewHolderCoin) return

        if (payloads.isEmpty()) {
            val item = items[position]
            holder.bind(item, expandedViewPosition == position)
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(items[position], expandedViewPosition == position)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderCoin(override val containerView: View, private val listener: BalanceCoinAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var balanceViewItem: BalanceViewItem? = null
    private var syncing = false

    init {
        containerView.setOnClickListener {
            balanceViewItem?.let {
                listener.onItemClicked(it)
            }
        }

        chartButton.setOnClickListener {
            balanceViewItem?.let {
                listener.onChartClicked(it)
            }
        }

        buttonPay.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSendClicked(it)
            }
        }

        buttonReceive.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onReceiveClicked(it)
            }
        }
    }

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean) {
        this.balanceViewItem = balanceViewItem

        balanceViewItem.apply {
            syncing = xSyncing
            iconProgress.visibility = xIconProgressVisibility
            xTextProgressText?.let {
                textProgress.text = it
            }
            buttonReceive.isEnabled = xButtonReceiveEnabled
            xIconProgressValue?.let {
                iconProgress.setProgress(it)
            }
            textSyncedUntil.text = xTextSyncedUntilText
            buttonPay.isEnabled = xButtonPayEnabled
            xCoinIconVisibility?.let {
                coinIcon.visibility = it
            }
            imgSyncFailed.visibility = xImgSyncFailedVisibility

            coinAmount.text = xCoinAmountText
            coinAmount.alpha = xCoinAmountAlpha

            coinTypeLabel.text = xTypeLabelText
            coinTypeLabel.background = xCoinTypeLabelBg

            showLockedBalance(balanceViewItem)

            coinIcon.bind(xIconDrawableResource, true)
            textCoinName.text = xTextCoinNameText

            exchangeRate.setTextColor(xExchangeRateTextColor)
            exchangeRate.text = xExchangeRateText
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE

        showChart(balanceViewItem, expanded)

        showFiatAmount(balanceViewItem, syncing && !expanded)
        updateSecondLineItemsVisibility(expanded)
    }

    private fun showLockedBalance(balanceViewItem: BalanceViewItem) {
        balanceViewItem.apply {
            coinAmountLocked.visibility = xCoinAmountLockedVisibility
            fiatAmountLocked.visibility = xFiatAmountLockedVisibility

            coinAmountLocked.text = xCoinAmountLockedText
            fiatAmountLocked.text = xFiatAmountLockedText

            xFiatAmountLockedAlpha?.let {
                fiatAmountLocked.alpha = it
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
        syncingStateGroup.visibility = if (syncing && !expanded) View.VISIBLE else View.GONE
        coinAmountGroup.visibility = if (syncing && !expanded) View.GONE else View.VISIBLE
    }

    private fun showFiatAmount(balanceViewItem: BalanceViewItem, collapsedAndSyncing: Boolean) {
        fiatAmount.visibility = when {
            collapsedAndSyncing -> View.GONE
            balanceViewItem.currencyValue == null -> View.GONE
            balanceViewItem.currencyValue.value.compareTo(BigDecimal.ZERO) == 0 -> View.GONE
            else -> View.VISIBLE
        }

        balanceViewItem.apply {
            fiatAmount.text = xFiatAmountText
            xFiatAmountAlpha?.let {
                fiatAmount.alpha = it
            }
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
