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
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_add_coin.*
import kotlinx.android.synthetic.main.view_holder_coin.*
import java.math.BigDecimal

class BalanceCoinAdapter(private val listener: Listener, private val viewDelegate: BalanceModule.IViewDelegate)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(position: Int)
        fun onReceiveClicked(position: Int)
        fun onItemClick(position: Int)
        fun onAddCoinClick()
        fun onClickChart(position: Int)
    }

    private val coinType = 1
    private val addCoinType = 2

    private var expandedViewPosition: Int? = null

    fun toggleViewHolder(position: Int) {
        expandedViewPosition?.let {
            notifyItemChanged(it, false)
        }

        if (expandedViewPosition != position) {
            notifyItemChanged(position, true)
        }

        expandedViewPosition = if (expandedViewPosition == position) null else position
    }

    override fun getItemCount() = viewDelegate.itemsCount + 1

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
            holder.manageCoins.setOnSingleClickListener { listener.onAddCoinClick() }
        }

        if (holder !is ViewHolderCoin) return
        val header = viewDelegate.getHeaderViewItem()
        val viewItem = viewDelegate.getViewItem(position)
        if (payloads.isEmpty()) {
            holder.bind(viewItem, expandedViewPosition == position, header.chartEnabled)
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(viewItem, expandedViewPosition == position, header.chartEnabled)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer
class ViewHolderCoin(override val containerView: View, private val listener: BalanceCoinAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var syncing = false

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean, chartEnabled: Boolean) {
        syncing = false
        buttonPay.isEnabled = false
        buttonReceive.isEnabled = true
        imgSyncFailed.visibility = View.GONE
        iconProgress.visibility = View.GONE

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

                    var progressText = containerView.context.getString(R.string.Balance_Syncing)
                    adapterState.lastBlockDate?.let {
                        progressText = containerView.context.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(it, "MMM d.yyyy"))
                    }

                    textProgress.text = progressText
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

        showFiatAmount(balanceViewItem)
        showChart(balanceViewItem, expanded, chartEnabled, fiatAmount.visibility)

        coinAmount.text = App.numberFormatter.format(balanceViewItem.coinValue)
        coinAmount.alpha = if (balanceViewItem.state is AdapterState.Synced) 1f else 0.3f

        textProgress.visibility = if (expanded && syncing) View.VISIBLE else View.GONE
        exchangeRate.visibility = if (expanded && syncing) View.GONE else View.VISIBLE

        coinIcon.bind(balanceViewItem.coin)
        textCoinName.text = balanceViewItem.coin.title

        exchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.steel_40 else R.color.grey))
        exchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
            containerView.context.getString(R.string.Balance_RatePerCoin, rateString, balanceViewItem.coin.code)
        }

        buttonPay.setOnSingleClickListener {
            listener.onSendClicked(adapterPosition)
        }

        chartButton.setOnClickListener { listener.onClickChart(adapterPosition) }

        buttonReceive.setOnSingleClickListener {
            listener.onReceiveClicked(adapterPosition)
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }

    fun bindPartial(balanceViewItem: BalanceViewItem, expanded: Boolean, chartEnabled: Boolean) {
        viewHolderRoot.isSelected = expanded
        textProgress.visibility = if (expanded && syncing) View.VISIBLE else View.GONE
        exchangeRate.visibility = if (expanded && syncing) View.GONE else View.VISIBLE

        if (expanded) {
            AnimationHelper.expand(buttonsWrapper)
        } else {
            AnimationHelper.collapse(buttonsWrapper)
        }

        showFiatAmount(balanceViewItem)
        showChart(balanceViewItem, expanded, chartEnabled, fiatAmount.visibility)
    }

    private fun showFiatAmount(balanceViewItem: BalanceViewItem) {
        balanceViewItem.currencyValue?.let {
            fiatAmount.text = App.numberFormatter.format(it, trimmable = true)
            fiatAmount.visibility = if (it.value.compareTo(BigDecimal.ZERO) == 0) View.GONE else View.VISIBLE
            fiatAmount.alpha = if (!balanceViewItem.rateExpired && balanceViewItem.state is AdapterState.Synced) 1f else 0.5f
        } ?: run {
            fiatAmount.visibility = View.GONE
        }
    }

    private fun showChart(viewItem: BalanceViewItem, expanded: Boolean, chartEnabled: Boolean, fiatAmountVisibility: Int) {
        if (expanded || !chartEnabled) {
            return setChartVisibility(false, fiatAmountVisibility)
        }

        setChartVisibility(true, fiatAmountVisibility)

        val chartData = viewItem.chartData
        if (chartData == null) {
            chartView.visibility = View.INVISIBLE
            chartRateDiff.text = containerView.context.getString(R.string.NotAvailable)
            chartRateDiff.setTextColor(containerView.context.getColor(R.color.grey_50))
        } else {
            val diffColor = if (chartData.diff < BigDecimal.ZERO)
                containerView.context.getColor(R.color.red_warning) else
                containerView.context.getColor(R.color.green_crypto)
            chartView.setData(chartData.points, ChartType.DAILY)
            chartRateDiff.text = App.numberFormatter.format(chartData.diff.toDouble(), showSign = true, precision = 2) + "%"
            chartRateDiff.setTextColor(diffColor)
        }
    }

    private fun setChartVisibility(show: Boolean, fiatAmountVisibility: Int) {
        if (show) {
            chartView.visibility = View.VISIBLE
            chartButton.visibility = View.VISIBLE
            chartViewCard.visibility = View.VISIBLE
            chartRateDiff.visibility = View.VISIBLE
            fiatAmount.visibility = View.GONE
            coinAmount.visibility = View.GONE
        } else {
            fiatAmount.visibility = fiatAmountVisibility
            coinAmount.visibility = View.VISIBLE
            chartViewCard.visibility = View.INVISIBLE
            chartButton.visibility = View.INVISIBLE
            chartRateDiff.visibility = View.INVISIBLE
        }
    }
}
