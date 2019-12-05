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
        this.balanceViewItem = item

        item.apply {
            if (xIconProgress == null) {
                iconProgress.visibility = View.INVISIBLE
            } else {
                iconProgress.setProgress(xIconProgress.toFloat())
                iconProgress.visibility = View.VISIBLE
            }

            coinIcon.bind(LayoutHelper.getCoinDrawableResource(coinIconCode), true)
            coinIcon.visibility = if (xCoinIconVisible) View.VISIBLE else View.GONE

            imgSyncFailed.visibility = if (xImgSyncFailedVisible) View.VISIBLE else View.GONE

            textCoinName.text = coinTitle

            exchangeRate.text = exchangeValue.value
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            diff?.let {
                txDiff.bind(it, containerView.context, false)
            }

            chartViewWrapper.visibility = if (xChartWrapperVisible) View.VISIBLE else View.GONE

            chartLoading.visibility = if (xChartLoadingVisible) View.VISIBLE else View.GONE
            textChartError.visibility = if (xChartErrorVisible) View.VISIBLE else View.GONE
            chartView.visibility = if (xChartVisible) View.VISIBLE else View.GONE

            xChartInfo?.let { chartInfo ->
                chartView.setData(chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }, ChartType.DAILY, chartInfo.startTimestamp, chartInfo.endTimestamp)
            }

            coinAmount.visibility = if (coinValue.visible) View.VISIBLE else View.INVISIBLE
            coinAmount.text = coinValue.value
            coinAmount.alpha = setDimmedAlpha(coinValue.dimmed, 0.3f)

            coinTypeLabel.text = coinLabel
            coinTypeLabel.visibility = if (xCoinTypeLabelVisible) View.VISIBLE else View.GONE

            fiatAmount.text = currencyValue.value
            fiatAmount.alpha = setDimmedAlpha(currencyValue.dimmed, 0.5f)
            fiatAmount.visibility = if (currencyValue.visible) View.VISIBLE else View.GONE

            coinAmountLocked.text = coinValueLocked.value
            coinAmountLocked.visibility = if (coinValueLocked.visible) View.VISIBLE else View.GONE

            fiatAmountLocked.text = currencyValueLocked.value
            fiatAmountLocked.alpha = setDimmedAlpha(currencyValueLocked.dimmed, 0.5f)
            fiatAmountLocked.visibility = if (currencyValueLocked.visible) View.VISIBLE else View.GONE

            textProgress.text = if (xTextProgress.value != null) {
                containerView.context.getString(R.string.Balance_Syncing_WithProgress, xTextProgress.value.toString())
            } else {
                containerView.context.getString(R.string.Balance_Syncing)
            }

            textSyncedUntil.text = containerView.context.getString(R.string.Balance_SyncedUntil, xTextProgress.until)
            syncingStateGroup.visibility = if (xTextProgress.visible) View.VISIBLE else View.GONE

            buttonReceive.isEnabled = xButtonReceiveEnabled
            buttonPay.isEnabled = xButtonPayEnabled

            buttonsWrapper.visibility = if (xExpanded) View.VISIBLE else View.GONE

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
            coinAmount.visibility = if (coinValue.visible) View.VISIBLE else View.INVISIBLE
            coinTypeLabel.visibility = if (xCoinTypeLabelVisible) View.VISIBLE else View.GONE
            fiatAmount.visibility = if (currencyValue.visible) View.VISIBLE else View.GONE
            syncingStateGroup.visibility = if (xTextProgress.visible) View.VISIBLE else View.GONE
            chartViewWrapper.visibility = if (xChartWrapperVisible) View.VISIBLE else View.GONE
            txDiff.visibility = if (xRateDiffVisible) View.VISIBLE else View.GONE

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
            if (xIconProgress == null) {
                iconProgress.visibility = View.INVISIBLE
            } else {
                iconProgress.setProgress(xIconProgress.toFloat())
                iconProgress.visibility = View.VISIBLE
            }

            textProgress.text = if (xTextProgress.value != null) {
                containerView.context.getString(R.string.Balance_Syncing_WithProgress, xTextProgress.value.toString())
            } else {
                containerView.context.getString(R.string.Balance_Syncing)
            }

            textSyncedUntil.text = containerView.context.getString(R.string.Balance_SyncedUntil, xTextProgress.until)
            syncingStateGroup.visibility = if (xTextProgress.visible) View.VISIBLE else View.GONE

            coinAmount.alpha = setDimmedAlpha(coinValue.dimmed, 0.3f)
            coinAmount.visibility = if (coinValue.visible) View.VISIBLE else View.INVISIBLE
            coinTypeLabel.visibility = if (xCoinTypeLabelVisible) View.VISIBLE else View.GONE
            fiatAmount.visibility = if (currencyValue.visible) View.VISIBLE else View.GONE
            fiatAmount.alpha = setDimmedAlpha(currencyValue.dimmed, 0.5f)

            buttonReceive.isEnabled = xButtonReceiveEnabled

            buttonPay.isEnabled = xButtonPayEnabled

            coinIcon.visibility = if (xCoinIconVisible) View.VISIBLE else View.GONE

            imgSyncFailed.visibility = if (xImgSyncFailedVisible) View.VISIBLE else View.GONE
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
            fiatAmount.alpha = setDimmedAlpha(currencyValue.dimmed, 0.5f)

            fiatAmountLocked.text = currencyValueLocked.value
            fiatAmountLocked.alpha = setDimmedAlpha(currencyValueLocked.dimmed, 0.5f)
        }
    }

    private fun bindUpdateChartInfo(item: BalanceViewItem) {
        item.apply {
            chartLoading.visibility = if (xChartLoadingVisible) View.VISIBLE else View.GONE
            textChartError.visibility = if (xChartErrorVisible) View.VISIBLE else View.GONE
            chartView.visibility = if (xChartVisible) View.VISIBLE else View.GONE

            xChartInfo?.let { chartInfo ->
                chartView.setData(chartInfo.points.map { ChartPoint(it.value.toFloat(), it.timestamp) }, ChartType.DAILY, chartInfo.startTimestamp, chartInfo.endTimestamp)
            }
        }
    }

    private fun setDimmedAlpha(dimmed: Boolean, alpha: Float): Float {
        return if (dimmed) alpha else 1f
    }
}
