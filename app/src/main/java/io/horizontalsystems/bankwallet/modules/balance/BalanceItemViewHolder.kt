package io.horizontalsystems.bankwallet.modules.balance

import android.view.View
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_balance_item.*
import java.math.BigDecimal

class BalanceItemViewHolder(override val containerView: View, private val listener: BalanceItemsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var balanceViewItem: BalanceViewItem? = null

    init {
        containerView.setOnClickListener {
            balanceViewItem?.let {
                listener.onItemClicked(it)
            }
        }

        buttonChart.setOnSingleClickListener {
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

        buttonSwap.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSwapClicked(it)
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

            iconCoin.setCoinImage(coinType)

            testnet.isVisible = !mainNet

            coinCodeTextView.text = coinCode
            coinLabel.text = coinTypeLabel

            balanceCoin.text = coinValue.text
            balanceFiat.text = fiatValue.text
            balanceCoinLocked.text = coinValueLocked.text
            balanceFiatLocked.text = fiatValueLocked.text

            exchangeRate.text = exchangeValue.text
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            setDiffPercentage(diff, rateDiff)
            setSyncProgressIcon(syncingProgress)

            buttonReceive.isEnabled = receiveEnabled
            buttonSend.isEnabled = sendEnabled
            buttonSwap.isVisible = swapVisible
            buttonSwap.isEnabled = swapEnabled

            balanceCoin.isVisible = coinValue.visible
            balanceFiat.isVisible = fiatValue.visible
            balanceCoinLocked.isVisible = coinValueLocked.visible
            balanceFiatLocked.isVisible = fiatValueLocked.visible
            lockedBorder.isVisible = coinValueLocked.visible

            balanceCoin.dimIf(coinValue.dimmed)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            iconCoin.isInvisible = !coinIconVisible
            iconNotSynced.isVisible = failedIconVisible

            coinLabel.isVisible = !coinTypeLabel.isNullOrBlank()
            exchangeRate.isInvisible = !exchangeValue.visible
            rateDiff.isInvisible = !exchangeValue.visible
            buttonChart.isEnabled = exchangeValue.text != null
        }

        BalanceCellAnimator.toggleButtons(this, item, false)
    }

    fun bindUpdate(current: BalanceViewItem, prev: BalanceViewItem) {
        if (current.expanded != prev.expanded) {
            BalanceCellAnimator.toggleButtons(this, current, true)
        }

        current.apply {

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
                buttonChart.isEnabled = exchangeValue.text != null
            }
            if (exchangeValue.dimmed != prev.exchangeValue.dimmed) {
                exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))
            }
            if (exchangeValue.visible != prev.exchangeValue.visible) {
                exchangeRate.isVisible = exchangeValue.visible
                rateDiff.isVisible = exchangeValue.visible
            }


            if (syncingTextValue.text != prev.syncingTextValue.text) {
                textSyncing.text = syncingTextValue.text
            }
            if (syncingTextValue.visible != prev.syncingTextValue.visible) {
                textSyncing.isVisible = syncingTextValue.visible
            }

            if (syncedUntilTextValue.text != prev.syncedUntilTextValue.text) {
                textSyncedUntil.text = syncedUntilTextValue.text
            }
            if (syncedUntilTextValue.visible != prev.syncedUntilTextValue.visible) {
                textSyncedUntil.isVisible = syncedUntilTextValue.visible
            }

            if (syncingProgress != prev.syncingProgress) {
                setSyncProgressIcon(syncingProgress)
            }
            if (diff != prev.diff) {
                setDiffPercentage(diff, rateDiff)
            }

            if (receiveEnabled != prev.receiveEnabled) {
                buttonReceive.isEnabled = receiveEnabled
            }
            if (sendEnabled != prev.sendEnabled) {
                buttonSend.isEnabled = sendEnabled
            }
            if (swapVisible != prev.swapVisible) {
                buttonSwap.isVisible = swapVisible
            }
            if (swapEnabled != prev.swapEnabled) {
                buttonSwap.isEnabled = swapEnabled
            }

            if (coinValue.visible != prev.coinValue.visible) {
                balanceCoin.isVisible = coinValue.visible
            }
            if (fiatValue.visible != prev.fiatValue.visible) {
                balanceFiat.isVisible = fiatValue.visible
            }
            if (coinValueLocked.visible != prev.coinValueLocked.visible) {
                balanceCoinLocked.isVisible = coinValueLocked.visible
                lockedBorder.isVisible = coinValueLocked.visible
            }
            if (fiatValueLocked.visible != prev.fiatValueLocked.visible) {
                balanceFiatLocked.isVisible = fiatValueLocked.visible
            }

            if (coinValue.dimmed != prev.coinValue.dimmed) {
                balanceCoin.dimIf(coinValue.dimmed)
            }
            if (fiatValue.dimmed != prev.fiatValue.dimmed) {
                balanceFiat.dimIf(fiatValue.dimmed)
            }
            if (coinValueLocked.dimmed != prev.coinValueLocked.dimmed) {
                balanceCoinLocked.dimIf(coinValueLocked.dimmed)
            }
            if (fiatValueLocked.dimmed != prev.fiatValueLocked.dimmed) {
                balanceFiatLocked.dimIf(fiatValueLocked.dimmed)
            }

            if (coinIconVisible != prev.coinIconVisible) {
                iconCoin.isInvisible = !coinIconVisible
            }
            if (failedIconVisible != prev.failedIconVisible) {
                iconNotSynced.isVisible = failedIconVisible
            }
        }
    }

    private fun setDiffPercentage(diff: BigDecimal?, view: TextView) {
        if (diff == null) {
            view.text = null
        } else {
            val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
            view.text = App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")

            val color = if (diff >= BigDecimal.ZERO) R.color.remus else R.color.lucian
            view.setTextColor(containerView.context.getColor(color))
        }
    }

    private fun setSyncProgressIcon(syncingProgress: SyncingProgress) {
        val color = if (syncingProgress.dimmed) R.color.grey_50 else R.color.grey
        syncingProgress.progress?.let {
            iconProgress.setProgressColored(it, itemView.context.getColor(color))
        }
        iconProgress.isVisible = syncingProgress.progress != null
    }

    private fun View.dimIf(condition: Boolean, dimmedAlpha: Float = 0.5f) {
        alpha = if (condition) dimmedAlpha else 1f
    }

    fun swipe() {
        balanceViewItem?.let {
            listener.onSwiped(it)
        }
    }
}
