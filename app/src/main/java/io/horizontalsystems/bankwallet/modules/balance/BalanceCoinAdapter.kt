package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_add_coin.*
import kotlinx.android.synthetic.main.view_holder_coin.*

class BalanceCoinAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            holder.bind(items[position])
        } else {
            holder.bindUpdate(items[position], payloads)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderCoin(override val containerView: View, private val listener: BalanceCoinAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var balanceViewItem: BalanceViewItem? = null

    init {
        containerView.setOnClickListener {
            balanceViewItem?.let {
                listener.onItemClicked(it)
            }
        }

        chartViewWrapper.setOnClickListener {
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

    fun bind(item: BalanceViewItem) {
        balanceViewItem = item

        item.apply {
            iconProgress.showIf(xIconProgress != null)
            xIconProgress?.let { iconProgress.setProgress(it.toFloat()) }

            coinIcon.bind(LayoutHelper.getCoinDrawableResource(coinIconCode), true)
            coinIcon.showIf(xCoinIconVisible)

            imgSyncFailed.showIf(xImgSyncFailedVisible)

            textCoinName.text = coinTitle

            exchangeRate.text = exchangeValue.value
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            diff?.let {
                txDiff.bind(it, containerView.context, false)
            }

            chartViewWrapper.showIf(xChartWrapperVisible)

            chartLoading.showIf(xChartLoadingVisible)
            textChartError.showIf(xChartErrorVisible)
            chartView.showIf(xChartVisible)

            xChartInfo?.let { chartInfo ->
                chartView.setData(chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }, ChartType.DAILY, chartInfo.startTimestamp, chartInfo.endTimestamp)
            }

            coinAmount.visibility = if (coinValue.visible) View.VISIBLE else View.INVISIBLE
            coinAmount.text = coinValue.value
            coinAmount.dimIf(coinValue.dimmed, 0.3f)

            coinTypeLabel.text = coinLabel
            coinTypeLabel.showIf(xCoinTypeLabelVisible)

            fiatAmount.text = currencyValue.value
            fiatAmount.dimIf(currencyValue.dimmed)
            fiatAmount.showIf(currencyValue.visible)

            coinAmountLocked.text = coinValueLocked.value
            coinAmountLocked.showIf(coinValueLocked.visible)

            fiatAmountLocked.text = currencyValueLocked.value
            fiatAmountLocked.dimIf(currencyValueLocked.dimmed)
            fiatAmountLocked.showIf(currencyValueLocked.visible)

            textProgress.text = if (xTextProgress.value != null) {
                containerView.context.getString(R.string.Balance_Syncing_WithProgress, xTextProgress.value.toString())
            } else {
                containerView.context.getString(R.string.Balance_Syncing)
            }

            textSyncedUntil.text = containerView.context.getString(R.string.Balance_SyncedUntil, xTextProgress.until)
            syncingStateGroup.showIf(xTextProgress.visible)

            buttonReceive.isEnabled = xButtonReceiveEnabled
            buttonPay.isEnabled = xButtonPayEnabled

            buttonsWrapper.showIf(xExpanded)

            containerView.isSelected = xExpanded
        }
    }

    fun bindUpdate(balanceViewItem: BalanceViewItem, payloads: MutableList<Any>) {
        payloads.forEach {
            when (it) {
                BalanceViewItem.UpdateType.EXPANDED -> bindUpdateExpanded(balanceViewItem)
                BalanceViewItem.UpdateType.STATE -> bindUpdateState(balanceViewItem)
                BalanceViewItem.UpdateType.BALANCE -> bindUpdateBalance(balanceViewItem)
                BalanceViewItem.UpdateType.MARKET_INFO -> bindUpdateMarketInfo(balanceViewItem)
                BalanceViewItem.UpdateType.CHART_INFO -> bindUpdateChartInfo(balanceViewItem)
            }
        }
    }

    private fun bindUpdateExpanded(item: BalanceViewItem) {
        item.apply {
            coinAmount.showIf(coinValue.visible, View.INVISIBLE)
            coinTypeLabel.showIf(xCoinTypeLabelVisible)
            fiatAmount.showIf(currencyValue.visible)
            syncingStateGroup.showIf(xTextProgress.visible)
            chartViewWrapper.showIf(xChartWrapperVisible)
            txDiff.showIf(xRateDiffVisible)

            containerView.isSelected = xExpanded

            if (xExpanded) {
                AnimationHelper.expand(buttonsWrapper)
            } else {
                AnimationHelper.collapse(buttonsWrapper)
            }
        }
    }

    private fun bindUpdateBalance(item: BalanceViewItem) {
        item.apply {
            coinAmount.text = coinValue.value
            fiatAmount.text = currencyValue.value

            coinAmountLocked.text = coinValueLocked.value
            fiatAmountLocked.text = currencyValueLocked.value

            buttonPay.isEnabled = xButtonPayEnabled
        }
    }

    private fun bindUpdateState(item: BalanceViewItem) {
        item.apply {
            iconProgress.showIf(xIconProgress != null)
            xIconProgress?.let { iconProgress.setProgress(it.toFloat()) }

            textProgress.text = if (xTextProgress.value != null) {
                containerView.context.getString(R.string.Balance_Syncing_WithProgress, xTextProgress.value.toString())
            } else {
                containerView.context.getString(R.string.Balance_Syncing)
            }

            textSyncedUntil.text = containerView.context.getString(R.string.Balance_SyncedUntil, xTextProgress.until)
            syncingStateGroup.showIf(xTextProgress.visible)

            coinAmount.dimIf(coinValue.dimmed, 0.3f)
            coinAmount.showIf(coinValue.visible, View.INVISIBLE)
            coinTypeLabel.showIf(xCoinTypeLabelVisible)
            fiatAmount.showIf(currencyValue.visible)
            fiatAmount.dimIf(currencyValue.dimmed)

            buttonReceive.isEnabled = xButtonReceiveEnabled

            buttonPay.isEnabled = xButtonPayEnabled

            coinIcon.showIf(xCoinIconVisible)

            imgSyncFailed.showIf(xImgSyncFailedVisible)
        }
    }

    private fun bindUpdateMarketInfo(item: BalanceViewItem) {
        item.apply {
            exchangeRate.text = exchangeValue.value
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            diff?.let {
                txDiff.bind(it, containerView.context, false)
            }

            fiatAmount.text = currencyValue.value
            fiatAmount.dimIf(currencyValue.dimmed)

            fiatAmountLocked.text = currencyValueLocked.value
            fiatAmountLocked.dimIf(currencyValueLocked.dimmed)
        }
    }

    private fun bindUpdateChartInfo(item: BalanceViewItem) {
        item.apply {
            chartLoading.showIf(xChartLoadingVisible)
            textChartError.showIf(xChartErrorVisible)
            chartView.showIf(xChartVisible)

            xChartInfo?.let { chartInfo ->
                chartView.setData(chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }, ChartType.DAILY, chartInfo.startTimestamp, chartInfo.endTimestamp)
            }
        }
    }

    private fun View.showIf(condition: Boolean, hideType: Int = View.GONE) {
        visibility = if (condition) View.VISIBLE else hideType
    }

    private fun View.dimIf(condition: Boolean, dimmedAlpha: Float = 0.5f) {
        alpha = if (condition) dimmedAlpha else 1f
    }
}
