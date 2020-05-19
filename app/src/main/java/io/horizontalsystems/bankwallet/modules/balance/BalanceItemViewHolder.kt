package io.horizontalsystems.bankwallet.modules.balance

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.setCoinImage
import io.horizontalsystems.views.showIf
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_balance_item.*

class BalanceItemViewHolder(override val containerView: View, private val listener: BalanceItemsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var balanceViewItem: BalanceViewItem? = null

    init {
        containerView.setOnClickListener {
            balanceViewItem?.let {
                listener.onItemClicked(it)
            }
        }

        rateDiffWrapper.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onChartClicked(it)
            }
        }

        buttonSend.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSendClicked(it)
            }
        }

        buttonReceive.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onReceiveClicked(it)
            }
        }

        iconNotSynced.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSyncErrorClicked(it)
            }
        }

        BalanceCellAnimator.measureHeights(this)
    }

    fun bind(item: BalanceViewItem) {
        balanceViewItem = item

        item.apply {
            syncSpinnerProgress?.let { iconProgress.setProgress(it.toFloat()) }

            iconCoin.setCoinImage(coinCode)

            coinName.text = coinTitle
            coinLabel.text = coinType

            balanceCoin.text = coinValue.text
            balanceFiat.text = fiatValue.text
            balanceCoinLocked.text = coinValueLocked.text
            balanceFiatLocked.text = fiatValueLocked.text

            exchangeRate.text = exchangeValue.text
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            setTextSyncing(syncingData)

            setRateDiff(item.diff)

            buttonReceive.isEnabled = receiveEnabled
            buttonSend.isEnabled = sendEnabled

            balanceCoin.showIf(coinValue.visible)
            balanceFiat.showIf(fiatValue.visible)
            balanceCoinLocked.showIf(coinValueLocked.visible)
            balanceFiatLocked.showIf(fiatValueLocked.visible)
            textSyncing.showIf(syncingData.syncingTextVisible)
            textSyncedUntil.showIf(syncingData.syncingTextVisible)

            balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            iconCoin.showIf(coinIconVisible)
            iconNotSynced.showIf(failedIconVisible)
            iconProgress.showIf(syncSpinnerProgress != null)

            coinLabel.showIf(coinTypeLabelVisible)
        }

        BalanceCellAnimator.toggleBalanceAndButtons(this, item)
    }

    fun bindUpdate(current: BalanceViewItem, prev: BalanceViewItem) {
        if (current.hideBalance != prev.hideBalance || current.expanded != prev.expanded) {
            BalanceCellAnimator.toggleBalanceAndButtonsAnimate(this, current, prev)
        }

        current.apply {
            if (prev.syncSpinnerProgress != syncSpinnerProgress) {
                syncSpinnerProgress?.let { iconProgress.setProgress(it.toFloat()) }
            }

            if (coinValue.text != prev.coinValue.text) {
                balanceCoin.text = coinValue.text
            }
            if (fiatValue.text != prev.fiatValue.text) {
                balanceFiat.text = fiatValue.text
            }

            if (coinValueLocked.text != prev.coinValueLocked.text) {
                balanceCoinLocked.text = coinValueLocked.text
            }
            if (fiatValueLocked.text != prev.fiatValueLocked.text) {
                balanceFiatLocked.text = fiatValueLocked.text
            }


            if (exchangeValue.text != prev.exchangeValue.text) {
                exchangeRate.text = exchangeValue.text
            }
            if (exchangeValue.dimmed != prev.exchangeValue.dimmed) {
                exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))
            }


            if (syncingData != prev.syncingData) {
                setTextSyncing(syncingData)
            }
            if (diff != prev.diff) {
                setRateDiff(diff)
            }

            if (receiveEnabled != prev.receiveEnabled) {
                buttonReceive.isEnabled = receiveEnabled
            }
            if (sendEnabled != prev.sendEnabled) {
                buttonSend.isEnabled = sendEnabled
            }

            if (coinValue.visible != prev.coinValue.visible) {
                balanceCoin.showIf(coinValue.visible)
            }
            if (fiatValue.visible != prev.fiatValue.visible) {
                balanceFiat.showIf(fiatValue.visible)
            }
            if (coinValueLocked.visible != prev.coinValueLocked.visible) {
                balanceCoinLocked.showIf(coinValueLocked.visible)
            }
            if (fiatValueLocked.visible != prev.fiatValueLocked.visible) {
                balanceFiatLocked.showIf(fiatValueLocked.visible)
            }
            if (syncingData.syncingTextVisible != prev.syncingData.syncingTextVisible) {
                textSyncing.showIf(syncingData.syncingTextVisible)
                textSyncedUntil.showIf(syncingData.syncingTextVisible)
            }

            if (coinValue.dimmed != prev.coinValue.dimmed) {
                balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            }
            if (fiatValue.dimmed != prev.fiatValue.dimmed) {
                balanceFiat.dimIf(fiatValue.dimmed)
            }
            if (coinValueLocked.dimmed != prev.coinValueLocked.dimmed) {
                balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            }
            if (fiatValueLocked.dimmed != prev.fiatValueLocked.dimmed) {
                balanceFiatLocked.dimIf(fiatValueLocked.dimmed)
            }

            if (coinIconVisible != prev.coinIconVisible) {
                iconCoin.showIf(coinIconVisible)
            }
            if (failedIconVisible != prev.failedIconVisible) {
                iconNotSynced.showIf(failedIconVisible)
            }
            if (syncSpinnerProgress != prev.syncSpinnerProgress) {
                iconProgress.showIf(syncSpinnerProgress != null)
            }
        }
    }

    private fun setRateDiff(rDiff: RateDiff) {
        rateDiff.text = rDiff.deemedValue.text ?: containerView.context.getString(R.string.NotAvailable)
        rateDiff.setTextColor(getRateDiffTextColor(rDiff.deemedValue.dimmed))
        rateDiffIcon.setImageResource(if (rDiff.positive) R.drawable.ic_up_green else R.drawable.ic_down_red)
        rateDiffIcon.imageTintList = getRateDiffTintList(rDiff.deemedValue.dimmed)
    }

    private fun getRateDiffTextColor(dimmed: Boolean): Int {
        return if (dimmed) {
            containerView.context.getColor(R.color.grey_50)
        } else {
            LayoutHelper.getAttr(R.attr.ColorLeah, containerView.context.theme)
                    ?: containerView.context.getColor(R.color.grey)
        }
    }

    private fun getRateDiffTintList(dimmed: Boolean): ColorStateList? {
        if (dimmed) {
            val greyColor = ContextCompat.getColor(containerView.context, R.color.grey_50)
            return ColorStateList.valueOf(greyColor)
        }
        return null
    }

    private fun setTextSyncing(syncingData: SyncingData) {
        textSyncing.text = if (syncingData.progress != null) {
            containerView.context.getString(R.string.Balance_Syncing_WithProgress, syncingData.progress.toString())
        } else if (syncingData.txCount != null) {
            containerView.context.getString(R.string.Balance_SearchingTransactions)
        } else {
            containerView.context.getString(R.string.Balance_Syncing)
        }

        textSyncedUntil.text = if (syncingData.until != null) {
            containerView.context.getString(R.string.Balance_SyncedUntil, syncingData.until)
        } else if (syncingData.txCount != null) {
            containerView.context.getString(R.string.Balance_FoundTx, syncingData.txCount)
        } else {
            ""
        }
    }

    private fun View.dimIf(condition: Boolean, dimmedAlpha: Float = 0.5f) {
        alpha = if (condition) dimmedAlpha else 1f
    }
}