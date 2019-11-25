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

    fun bind(balanceViewItem: BalanceViewItem) {
        this.balanceViewItem = balanceViewItem

        balanceViewItem.apply {
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
            coinIcon.visibility = xCoinIconVisibility
            imgSyncFailed.visibility = xImgSyncFailedVisibility

            coinAmount.text = xCoinAmountText
            coinAmount.alpha = xCoinAmountAlpha

            coinTypeLabel.text = xTypeLabelText

            showLockedBalance(balanceViewItem)

            coinIcon.bind(xIconDrawableResource, true)
            textCoinName.text = xTextCoinNameText

            exchangeRate.setTextColor(xExchangeRateTextColor)
            exchangeRate.text = xExchangeRateText

            containerView.isSelected = xExpanded
            buttonsWrapper.visibility = xButtonsWrapperVisibility

            showChart(balanceViewItem)

            showFiatAmount(balanceViewItem)
            updateSecondLineItemsVisibility(balanceViewItem)
        }
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

    fun bindUpdate(balanceViewItem: BalanceViewItem, payloads: MutableList<Any>) {
        payloads.forEach {
            when (it) {
                BalanceViewItem.UpdateType.EXPANDED -> {
                    balanceViewItem.apply {
                        containerView.isSelected = xExpanded

                        showFiatAmount(balanceViewItem)
                        updateSecondLineItemsVisibility(balanceViewItem)

                        if (xExpanded) {
                            AnimationHelper.expand(buttonsWrapper)
                        } else {
                            AnimationHelper.collapse(buttonsWrapper)
                        }

                        showChart(balanceViewItem)
                    }
                }
                BalanceViewItem.UpdateType.STATE -> {
                    balanceViewItem.apply {
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
                        coinIcon.visibility = xCoinIconVisibility
                        imgSyncFailed.visibility = xImgSyncFailedVisibility

                        coinAmount.alpha = xCoinAmountAlpha

                        xFiatAmountLockedAlpha?.let {
                            fiatAmountLocked.alpha = it
                        }

                        fiatAmount.visibility = xFiatAmountVisibility

                        xFiatAmountAlpha?.let {
                            fiatAmount.alpha = it
                        }

                        syncingStateGroup.visibility = xSyncingStateGroupVisibility
                        coinAmount.visibility = xCoinAmountVisibility
                        coinTypeLabel.visibility = xCoinTypeLabelVisibility
                    }
                }
                BalanceViewItem.UpdateType.BALANCE -> {
                    balanceViewItem.apply {
                        coinAmount.text = xCoinAmountText

                        showFiatAmount(balanceViewItem)
                        showLockedBalance(balanceViewItem)

                        buttonPay.isEnabled = xButtonPayEnabled
                    }
                }
                BalanceViewItem.UpdateType.MARKET_INFO -> {
                    balanceViewItem.apply {
                        exchangeRate.setTextColor(xExchangeRateTextColor)
                        exchangeRate.text = xExchangeRateText

                        showFiatAmount(balanceViewItem)

                        fiatAmountLocked.visibility = xFiatAmountLockedVisibility
                        fiatAmountLocked.text = xFiatAmountLockedText
                        xFiatAmountLockedAlpha?.let {
                            fiatAmountLocked.alpha = it
                        }
                    }
                }
                BalanceViewItem.UpdateType.CHART_INFO -> {
                    showChart(balanceViewItem)
                }
            }
        }
    }

    private fun updateSecondLineItemsVisibility(balanceViewItem: BalanceViewItem) {
        balanceViewItem.apply {
            syncingStateGroup.visibility = xSyncingStateGroupVisibility
            coinAmount.visibility = xCoinAmountVisibility
            coinTypeLabel.visibility = xCoinTypeLabelVisibility
        }
    }

    private fun showFiatAmount(balanceViewItem: BalanceViewItem) {
        balanceViewItem.apply {
            fiatAmount.visibility = xFiatAmountVisibility
            fiatAmount.text = xFiatAmountText

            xFiatAmountAlpha?.let {
                fiatAmount.alpha = it
            }
        }
    }

    private fun showChart(viewItem: BalanceViewItem) {
        if (viewItem.xExpanded || viewItem.chartInfoState !is ChartInfoState.Loaded) {
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
            chartViewWrapper.visibility = View.VISIBLE
            txDiff.visibility = View.VISIBLE
        } else {
            chartViewWrapper.visibility = View.GONE
            txDiff.visibility = View.INVISIBLE
        }
    }
}
