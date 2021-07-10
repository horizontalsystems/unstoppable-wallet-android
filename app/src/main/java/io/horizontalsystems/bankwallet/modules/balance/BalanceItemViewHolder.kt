package io.horizontalsystems.bankwallet.modules.balance

import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setCoinImage
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import kotlinx.android.extensions.LayoutContainer

class BalanceItemViewHolder(override val containerView: View, private val listener: BalanceItemsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var balanceViewItem: BalanceViewItem? = null
    private var buttonSend: Button
    private var buttonReceive: Button
    private var buttonSwap: Button
    private var iconNotSynced: ImageView
    private var iconCoin: ImageView
    private var coinName: TextView
    private var coinLabel: TextView
    private var balanceCoin: TextView
    private var balanceFiat: TextView
    var balanceCoinLocked: TextView
    var balanceFiatLocked: TextView
    private var exchangeRate: TextView
    private var rateDiff: TextView
    private var rateDiffIcon: ImageView
    private var iconProgress: RotatingCircleProgressView
    private var textSyncing: TextView
    private var textSyncedUntil: TextView
    var balanceWrapper: FrameLayout
    var lockedBalanceWrapper: FrameLayout
    var buttonsWrapper: ConstraintLayout
    var border: FrameLayout
    var rootWrapper: ConstraintLayout

    init {
        containerView.setOnClickListener {
            balanceViewItem?.let {
                listener.onItemClicked(it)
            }
        }

        containerView.findViewById<FrameLayout>(R.id.rateDiffWrapper).setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onChartClicked(it)
            }
        }

        buttonSend = containerView.findViewById(R.id.buttonSend)
        buttonSend.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSendClicked(it)
            }
        }

        buttonReceive = containerView.findViewById(R.id.buttonReceive)
        buttonReceive.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onReceiveClicked(it)
            }
        }

        buttonSwap = containerView.findViewById(R.id.buttonSwap)
        buttonSwap.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSwapClicked(it)
            }
        }

        iconNotSynced = containerView.findViewById(R.id.iconNotSynced)
        iconNotSynced.setOnSingleClickListener {
            balanceViewItem?.let {
                listener.onSyncErrorClicked(it)
            }
        }

        iconCoin = containerView.findViewById(R.id.iconCoin)
        coinName = containerView.findViewById(R.id.coinName)
        coinLabel = containerView.findViewById(R.id.coinLabel)
        balanceCoin = containerView.findViewById(R.id.balanceCoin)
        balanceFiat = containerView.findViewById(R.id.balanceFiat)
        balanceCoinLocked = containerView.findViewById(R.id.balanceCoinLocked)
        balanceFiatLocked = containerView.findViewById(R.id.balanceFiatLocked)
        exchangeRate = containerView.findViewById(R.id.exchangeRate)
        rateDiff = containerView.findViewById(R.id.rateDiff)
        rateDiffIcon = containerView.findViewById(R.id.rateDiffIcon)
        iconProgress = containerView.findViewById(R.id.iconProgress)
        textSyncing = containerView.findViewById(R.id.textSyncing)
        textSyncedUntil = containerView.findViewById(R.id.textSyncedUntil)
        balanceWrapper = containerView.findViewById(R.id.balanceWrapper)
        lockedBalanceWrapper = containerView.findViewById(R.id.lockedBalanceWrapper)
        buttonsWrapper = containerView.findViewById(R.id.buttonsWrapper)
        border = containerView.findViewById(R.id.border)
        rootWrapper = containerView.findViewById(R.id.rootWrapper)

        BalanceCellAnimator.measureHeights(this)
    }

    fun bind(item: BalanceViewItem) {
        balanceViewItem = item

        item.apply {

            iconCoin.setCoinImage(coinType)

            coinName.text = coinTitle
            coinLabel.text = coinTypeLabel

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
            buttonSwap.isVisible = swapVisible
            buttonSwap.isEnabled = swapEnabled

            balanceCoin.isVisible = coinValue.visible
            balanceFiat.isVisible = fiatValue.visible
            balanceCoinLocked.isVisible = coinValueLocked.visible
            balanceFiatLocked.isVisible = fiatValueLocked.visible

            balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            iconCoin.isVisible = coinIconVisible
            iconNotSynced.isVisible = failedIconVisible

            coinLabel.isVisible = !coinTypeLabel.isNullOrBlank()
        }

        BalanceCellAnimator.toggleBalanceAndButtons(this, item)
    }

    fun bindUpdate(current: BalanceViewItem, prev: BalanceViewItem) {
        if (current.hideBalance != prev.hideBalance || current.expanded != prev.expanded) {
            BalanceCellAnimator.toggleBalanceAndButtonsAnimate(this, current, prev)
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
            }
            if (fiatValueLocked.visible != prev.fiatValueLocked.visible) {
                balanceFiatLocked.isVisible = fiatValueLocked.visible
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
                iconCoin.isVisible = coinIconVisible
            }
            if (failedIconVisible != prev.failedIconVisible) {
                iconNotSynced.isVisible = failedIconVisible
            }
        }
    }

    private fun setRateDiff(rDiff: RateDiff) {
        rateDiff.text = rDiff.deemedValue.text ?: containerView.context.getString(R.string.NotAvailable)
        rateDiff.setTextColor(getRateDiffTextColor(rDiff.deemedValue.dimmed))
        rateDiffIcon.setImageResource(if (rDiff.positive) R.drawable.ic_up_green_20 else R.drawable.ic_down_red_20)
        rateDiffIcon.imageTintList = getRateDiffTintList(rDiff.deemedValue.dimmed)
    }

    private fun getRateDiffTextColor(dimmed: Boolean): Int {
        return containerView.context.getColor(if (dimmed) R.color.grey_50 else R.color.leah)
    }

    private fun getRateDiffTintList(dimmed: Boolean): ColorStateList? {
        if (dimmed) {
            val greyColor = ContextCompat.getColor(containerView.context, R.color.grey_50)
            return ColorStateList.valueOf(greyColor)
        }
        return null
    }

    private fun setTextSyncing(syncingData: SyncingData?) {
        when (syncingData) {
            is SyncingData.Blockchain -> {
                iconProgress.setProgressColored(syncingData.spinnerProgress, itemView.context.getColor(R.color.grey))
                iconProgress.isVisible = true

                textSyncing.isVisible = syncingData.syncingTextVisible
                textSyncedUntil.isVisible = syncingData.syncingTextVisible


                textSyncing.text = if (syncingData.progress != null) {
                    containerView.context.getString(R.string.Balance_Syncing_WithProgress, syncingData.progress.toString())
                } else {
                    containerView.context.getString(R.string.Balance_Syncing)
                }

                textSyncedUntil.text = if (syncingData.until != null) {
                    containerView.context.getString(R.string.Balance_SyncedUntil, syncingData.until)
                } else {
                    null
                }
            }
            is SyncingData.SearchingTxs -> {
                iconProgress.setProgressColored(10, itemView.context.getColor(R.color.grey_50))
                iconProgress.isVisible = true

                textSyncing.isVisible = syncingData.syncingTextVisible
                textSyncedUntil.isVisible = syncingData.syncingTextVisible

                textSyncing.text = containerView.context.getString(R.string.Balance_SearchingTransactions)
                textSyncedUntil.text = if (syncingData.txCount > 0) {
                    containerView.context.getString(R.string.Balance_FoundTx, syncingData.txCount.toString())
                } else {
                    null
                }
            }
            null -> {
                iconProgress.isVisible = false
                textSyncing.isVisible = false
                textSyncedUntil.isVisible = false

                textSyncing.text = null
                textSyncedUntil.text = null
            }
        }
    }

    private fun View.dimIf(condition: Boolean, dimmedAlpha: Float = 0.5f) {
        alpha = if (condition) dimmedAlpha else 1f
    }
}
