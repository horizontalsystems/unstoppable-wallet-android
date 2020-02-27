package io.horizontalsystems.bankwallet.modules.balance

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.views.LayoutHelper
import io.horizontalsystems.views.helpers.AnimationHelper
import io.horizontalsystems.views.inflate
import io.horizontalsystems.views.showIf
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
        //  Update with regular method for the initial load to avoid showing balance tab with empty list
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

        rateDiffWrapper.setOnClickListener {
            balanceViewItem?.let {
                if (!it.blockChart) {
                    listener.onChartClicked(it)
                }
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
    }

    fun bind(item: BalanceViewItem) {
        balanceViewItem = item

        item.apply {
            syncingData.progress?.let { iconProgress.setProgress(it.toFloat()) }

            iconCoin.bind(coinCode)

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

            containerView.isSelected = expanded

            balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            iconCoin.showIf(coinIconVisible)
            iconNotSynced.showIf(failedIconVisible)
            iconProgress.showIf(syncingData.progress != null)

            balanceCoin.showIf(coinValue.visible, View.INVISIBLE)
            balanceFiat.showIf(fiatValue.visible)
            coinLabel.showIf(coinTypeLabelVisible)

            balanceCoinLocked.showIf(coinValueLocked.visible)
            balanceFiatLocked.showIf(fiatValueLocked.visible)

            textSyncingGroup.showIf(syncingData.syncingTextVisible)

            buttonsWrapper.showIf(expanded)
        }
    }

    fun bindUpdate(balanceViewItem: BalanceViewItem, payloads: MutableList<Any>) {
        payloads.forEach {
            when (it) {
                BalanceViewItem.UpdateType.EXPANDED -> bindUpdateExpanded(balanceViewItem)
                BalanceViewItem.UpdateType.STATE -> bindUpdateState(balanceViewItem)
                BalanceViewItem.UpdateType.BALANCE -> bindUpdateBalance(balanceViewItem)
                BalanceViewItem.UpdateType.MARKET_INFO -> bindUpdateMarketInfo(balanceViewItem)
            }
        }
    }

    private fun bindUpdateExpanded(item: BalanceViewItem) {
        item.apply {
            balanceCoin.showIf(coinValue.visible, View.INVISIBLE)
            balanceFiat.showIf(fiatValue.visible)

            coinLabel.showIf(coinTypeLabelVisible)
            textSyncingGroup.showIf(syncingData.syncingTextVisible)

            containerView.isSelected = expanded

            if (expanded) {
                AnimationHelper.expand(buttonsWrapper)
            } else {
                AnimationHelper.collapse(buttonsWrapper)
            }
        }
    }

    private fun bindUpdateBalance(item: BalanceViewItem) {
        item.apply {
            balanceCoin.text = coinValue.text
            balanceFiat.text = fiatValue.text

            balanceCoinLocked.text = coinValueLocked.text
            balanceFiatLocked.text = fiatValueLocked.text

            balanceCoinLocked.showIf(coinValueLocked.visible)
            balanceFiatLocked.showIf(fiatValueLocked.visible)
        }
    }

    private fun bindUpdateState(item: BalanceViewItem) {
        item.apply {
            iconProgress.showIf(syncingData.progress != null)
            syncingData.progress?.let { iconProgress.setProgress(it.toFloat()) }

            iconCoin.showIf(coinIconVisible)
            iconNotSynced.showIf(failedIconVisible)
            textSyncingGroup.showIf(syncingData.syncingTextVisible)
            setTextSyncing(syncingData)

            balanceCoin.showIf(coinValue.visible, View.INVISIBLE)
            coinLabel.showIf(coinTypeLabelVisible)
            balanceFiat.showIf(fiatValue.visible)

            balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            buttonReceive.isEnabled = receiveEnabled
            buttonSend.isEnabled = sendEnabled
        }
    }

    private fun bindUpdateMarketInfo(item: BalanceViewItem) {
        item.apply {
            exchangeRate.text = exchangeValue.text
            exchangeRate.setTextColor(containerView.context.getColor(if (exchangeValue.dimmed) R.color.grey_50 else R.color.grey))

            setRateDiff(item.diff)

            balanceFiat.text = fiatValue.text
            balanceFiat.dimIf(fiatValue.dimmed)

            balanceFiatLocked.text = fiatValueLocked.text
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)
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
            LayoutHelper.getAttr(R.attr.ColorLeah, containerView.context.theme) ?: containerView.context.getColor(R.color.grey)
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
        textSyncing.text = if (syncingData.progress == null) {
            containerView.context.getString(R.string.Balance_Syncing)
        } else {
            containerView.context.getString(R.string.Balance_Syncing_WithProgress, syncingData.progress.toString())
        }

        if (syncingData.until != null) {
            textSyncedUntil.text = containerView.context.getString(R.string.Balance_SyncedUntil, syncingData.until)
        } else {
            textSyncedUntil.text = ""
        }
    }

    private fun View.dimIf(condition: Boolean, dimmedAlpha: Float = 0.5f) {
        alpha = if (condition) dimmedAlpha else 1f
    }
}
