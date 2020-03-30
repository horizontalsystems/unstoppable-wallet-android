package io.horizontalsystems.bankwallet.modules.balance

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.*
import io.horizontalsystems.views.helpers.LayoutHelper
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

    private val context = App.instance
    private var collapsedHeight = -1
    private var expandedHeight = -1
    private var expandedHeightBalanceHidden = -1
    private var collapsedBalanceHiddenHeight = -1
    private var lockedBalanceWrapperHeight = -1
    private var balanceWrapperHeight = -1

    private lateinit var recyclerView: RecyclerView

    private val animationPlaybackSpeed: Double = 1.3
    private val listItemExpandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    private fun setViewHeights(context: Context) {
        val view = View.inflate(context, R.layout.view_holder_coin, null)
        balanceWrapperHeight = view.findViewById<FrameLayout>(R.id.balanceWrapper)?.measureHeight()
                ?: -1
        lockedBalanceWrapperHeight = view.findViewById<FrameLayout>(R.id.lockedBalanceWrapper)?.measureHeight()
                ?: -1
        val buttonsWrapperHeight = view.findViewById<ConstraintLayout>(R.id.buttonsWrapper)?.measureHeight()
                ?: -1
        val borderHeight = view.findViewById<FrameLayout>(R.id.border)?.measureHeight() ?: -1

        expandedHeight = view.measureHeight()
        expandedHeightBalanceHidden = expandedHeight - balanceWrapperHeight - lockedBalanceWrapperHeight

        collapsedHeight = expandedHeight - buttonsWrapperHeight
        collapsedBalanceHiddenHeight = expandedHeight - buttonsWrapperHeight - balanceWrapperHeight - borderHeight
    }

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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        if (balanceWrapperHeight <= 0) {
            setViewHeights(recyclerView.context)
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
            applyChange(holder, items[position], false, null)
        } else {
            val item = items[position]
            holder.bindUpdate(items[position], payloads)
            if (payloads.contains(BalanceViewItem.UpdateType.TOGGLEBALANCE)) {
                applyChange(holder, item, true, BalanceViewItem.UpdateType.TOGGLEBALANCE)
            } else if (payloads.contains(BalanceViewItem.UpdateType.EXPANDED)) {
                applyChange(holder, item, true, BalanceViewItem.UpdateType.EXPANDED)
            }
        }
    }

    private fun applyChange(holder: ViewHolderCoin, viewItem: BalanceViewItem, animate: Boolean, changeType: BalanceViewItem.UpdateType?) {
        if (animate) {

            val animationForward = when (changeType) {
                BalanceViewItem.UpdateType.TOGGLEBALANCE -> !viewItem.hideBalance
                BalanceViewItem.UpdateType.EXPANDED -> viewItem.expanded
                else -> true
            }

            var heightSmall = 0
            var heightBig = 0

            when (changeType) {
                BalanceViewItem.UpdateType.TOGGLEBALANCE -> {
                    heightSmall = getHeight(viewItem.expanded, true, viewItem.coinValueLocked.visible)
                    heightBig = getHeight(viewItem.expanded, false, viewItem.coinValueLocked.visible)
                }
                BalanceViewItem.UpdateType.EXPANDED -> {
                    heightSmall = getHeight(false, viewItem.hideBalance, viewItem.coinValueLocked.visible)
                    heightBig = getHeight(true, viewItem.hideBalance, viewItem.coinValueLocked.visible)
                }
                else -> { }
            }

            val rootAnimator =
                    if (animationForward) ValueAnimator.ofFloat(0f, 1f)
                    else ValueAnimator.ofFloat(1f, 0f)

            rootAnimator.duration = listItemExpandDuration
            rootAnimator.interpolator = AccelerateDecelerateInterpolator()
            rootAnimator.addUpdateListener { valueAnimator ->
                val progress = valueAnimator.animatedValue as Float

                if (changeType == BalanceViewItem.UpdateType.TOGGLEBALANCE) {
                    setExpandProgress(holder.balanceWrapper, 0, balanceWrapperHeight, progress)
                    if (viewItem.coinValueLocked.visible) {
                        setExpandProgress(holder.lockedBalanceWrapper, 0, lockedBalanceWrapperHeight, progress)
                    }
                }

                setExpandProgress(holder.rootWrapper, heightSmall, heightBig, progress)
            }

            if (animationForward) {
                rootAnimator.doOnStart {
                    when (changeType) {
                        BalanceViewItem.UpdateType.EXPANDED -> {
                            holder.buttonsWrapper.isVisible = true
                        }
                        BalanceViewItem.UpdateType.TOGGLEBALANCE -> {
                            holder.buttonsWrapper.isVisible = true
                            if (viewItem.coinValueLocked.visible && !holder.lockedBalanceWrapper.isVisible) {
                                holder.lockedBalanceWrapper.isVisible = true
                            }
                        }
                    }
                }
                rootAnimator.doOnEnd {
                    when (changeType) {
                        BalanceViewItem.UpdateType.TOGGLEBALANCE -> {
                            if (viewItem.coinValueLocked.visible) {
                                holder.balanceCoinLocked.isVisible = true
                                holder.balanceFiatLocked.isVisible = true
                            }
                        }
                    }
                }
            } else {
                rootAnimator.doOnStart {
                    when (changeType) {
                        BalanceViewItem.UpdateType.TOGGLEBALANCE -> {
                            if (viewItem.coinValueLocked.visible) {
                                holder.balanceCoinLocked.isVisible = false
                                holder.balanceFiatLocked.isVisible = false
                            }
                        }
                    }
                }
                rootAnimator.doOnEnd {
                    when (changeType) {
                        BalanceViewItem.UpdateType.EXPANDED -> {
                            holder.buttonsWrapper.isVisible = false
                        }
                    }
                }
            }

            rootAnimator.start()
    } else {
            val smallHeight = getHeight(false, true, viewItem.coinValueLocked.visible)
            val bigHeight = getHeight(viewItem.expanded, viewItem.hideBalance, viewItem.coinValueLocked.visible)

            holder.lockedBalanceWrapper.isVisible = viewItem.coinValueLocked.visible && !viewItem.hideBalance

            if (viewItem.coinValueLocked.visible) {
                holder.balanceCoinLocked.isVisible = !viewItem.hideBalance
                holder.balanceFiatLocked.isVisible = !viewItem.hideBalance
            }

            setExpandProgress(holder.balanceWrapper, 0, balanceWrapperHeight, if(!viewItem.hideBalance) 1f else 0f)

            val forwardAnimation = viewItem.expanded || !viewItem.hideBalance
            setExpandProgress(holder.rootWrapper, smallHeight, bigHeight, if (forwardAnimation) 1f else 0f)
        }
    }

    private fun getHeight(expanded: Boolean, balanceHidden: Boolean, showLocked: Boolean): Int {
        if (expanded) {
            if (balanceHidden) {
                return expandedHeightBalanceHidden
            } else {
                return if (showLocked) expandedHeight else expandedHeight - lockedBalanceWrapperHeight
            }
        } else {
            return if (balanceHidden) collapsedBalanceHiddenHeight else collapsedHeight
        }
    }

    private fun setExpandProgress(view: View, smallHeight: Int, bigHeight: Int, progress: Float) {
        view.layoutParams.height = (smallHeight + (bigHeight - smallHeight) * progress).toInt()
        view.requestLayout()
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

            balanceCoin.showIf(coinValue.visible)
            balanceFiat.showIf(fiatValue.visible)
            balanceCoinLocked.showIf(coinValueLocked.visible, View.INVISIBLE)
            balanceFiatLocked.showIf(fiatValueLocked.visible, View.INVISIBLE)
            textSyncing.showIf(syncingData.syncingTextVisible)
            textSyncedUntil.showIf(syncingData.syncingTextVisible)

            balanceCoin.dimIf(coinValue.dimmed, 0.3f)
            balanceFiat.dimIf(fiatValue.dimmed)
            balanceCoinLocked.dimIf(coinValueLocked.dimmed, 0.3f)
            balanceFiatLocked.dimIf(fiatValueLocked.dimmed)

            iconCoin.showIf(coinIconVisible)
            iconNotSynced.showIf(failedIconVisible)
            iconProgress.showIf(syncingData.progress != null)

            coinLabel.showIf(coinTypeLabelVisible)
        }
    }

    fun bindUpdate(balanceViewItem: BalanceViewItem, payloads: MutableList<Any>) {
        payloads.forEach {
            when (it) {
                BalanceViewItem.UpdateType.STATE -> bindUpdateState(balanceViewItem)
                BalanceViewItem.UpdateType.BALANCE -> bindUpdateBalance(balanceViewItem)
                BalanceViewItem.UpdateType.MARKET_INFO -> bindUpdateMarketInfo(balanceViewItem)
            }
        }
    }

    private fun bindUpdateBalance(item: BalanceViewItem) {
        item.apply {
            balanceCoin.text = coinValue.text
            balanceFiat.text = fiatValue.text

            balanceCoinLocked.text = coinValueLocked.text
            balanceFiatLocked.text = fiatValueLocked.text

            balanceCoinLocked.showIf(coinValueLocked.visible, View.INVISIBLE)
            balanceFiatLocked.showIf(fiatValueLocked.visible, View.INVISIBLE)
        }
    }

    private fun bindUpdateState(item: BalanceViewItem) {
        item.apply {
            iconProgress.showIf(syncingData.progress != null)
            syncingData.progress?.let { iconProgress.setProgress(it.toFloat()) }

            iconCoin.showIf(coinIconVisible)
            iconNotSynced.showIf(failedIconVisible)
            setTextSyncing(syncingData)

            balanceCoin.showIf(coinValue.visible)
            balanceFiat.showIf(fiatValue.visible)
            balanceCoinLocked.showIf(coinValueLocked.visible && !hideBalance)
            balanceFiatLocked.showIf(fiatValueLocked.visible && !hideBalance)
            textSyncing.showIf(syncingData.syncingTextVisible)
            textSyncedUntil.showIf(syncingData.syncingTextVisible)

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
