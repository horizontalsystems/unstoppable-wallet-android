package io.horizontalsystems.bankwallet.modules.balance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.main.MainActivity
import io.horizontalsystems.bankwallet.modules.managecoins.ManageCoinsModule
import io.horizontalsystems.bankwallet.ui.extensions.NpaLinearLayoutManager
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_balance.*
import kotlinx.android.synthetic.main.view_holder_add_coin.*
import kotlinx.android.synthetic.main.view_holder_coin.*
import java.math.BigDecimal
import com.google.android.material.appbar.AppBarLayout
import android.view.ViewTreeObserver
import kotlinx.android.synthetic.main.fragment_balance.app_bar_layout
import kotlinx.android.synthetic.main.fragment_balance.toolbarTitle


class BalanceFragment : Fragment(), CoinsAdapter.Listener {

    private lateinit var viewModel: BalanceViewModel
    private var coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbarTitle.setText(R.string.Balance_Title)

        viewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        viewModel.init()

        viewModel.openReceiveDialog.observe(viewLifecycleOwner, Observer { coinCode ->
            coinCode?.let {
                (activity as? MainActivity)?.openReceiveDialog(it)
            }
        })

        viewModel.openSendDialog.observe(viewLifecycleOwner, Observer { coinCode ->
            coinCode?.let {
                (activity as? MainActivity)?.openSendDialog(it)
            }
        })

        viewModel.balanceColorLiveDate.observe(viewLifecycleOwner, Observer { color ->
            color?.let { colorRes ->
                context?.let { it ->
                    ballanceText.setTextColor(ContextCompat.getColor(it, colorRes))
                }
            }
        })

        viewModel.didRefreshLiveEvent.observe(viewLifecycleOwner, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.openManageCoinsLiveEvent.observe(viewLifecycleOwner, Observer {
            context?.let { context -> ManageCoinsModule.start(context) }
        })

        viewModel.reloadLiveEvent.observe(viewLifecycleOwner, Observer {
            coinsAdapter.notifyDataSetChanged()
            reloadHeader()
        })

        viewModel.reloadHeaderLiveEvent.observe(viewLifecycleOwner, Observer {
            reloadHeader()
        })

        viewModel.reloadItemLiveEvent.observe(viewLifecycleOwner, Observer { position ->
            position?.let {
                coinsAdapter.notifyItemChanged(it)
            }
        })


        coinsAdapter.viewDelegate = viewModel.delegate
        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = NpaLinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }

        activity?.theme?.let { theme ->
            LayoutHelper.getAttr(R.attr.SwipeRefreshBackgroundColor, theme)?.let { color ->
                pullToRefresh.setProgressBackgroundColorSchemeColor(color)
            }
            LayoutHelper.getAttr(R.attr.SwipeRefreshSpinnerColor, theme)?.let { color ->
                pullToRefresh.setColorSchemeColors(color)
            }
        }

        setAppBarAnimation()
    }

    private fun setAppBarAnimation() {
        toolbarTitle.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                toolbarTitle.pivotX = 0f
                toolbarTitle.pivotY = toolbarTitle.height.toFloat()
                toolbarTitle.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        app_bar_layout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val fraction = Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange
            var alphaFract = 1f - fraction
            if (alphaFract < 0.20) {
                alphaFract = 0f
            }
            toolbarTitle.alpha = alphaFract
            toolbarTitle.scaleX = (1f - fraction / 3)
            toolbarTitle.scaleY = (1f - fraction / 3)
        })
    }

    override fun onResume() {
        super.onResume()
        coinsAdapter.notifyDataSetChanged()
    }

    private fun reloadHeader() {
        val headerViewItem = viewModel.delegate.getHeaderViewItem()

        context?.let {
            val color = if (headerViewItem.upToDate) R.color.yellow_crypto else R.color.yellow_crypto_40
            ballanceText.setTextColor(ContextCompat.getColor(it, color))
        }

        ballanceText.text = headerViewItem.currencyValue?.let {
            App.numberFormatter.format(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    override fun onSendClicked(position: Int) {
        viewModel.onSendClicked(position)
    }

    override fun onReceiveClicked(position: Int) {
        viewModel.onReceiveClicked(position)
    }

    override fun onItemClick(position: Int) {
        coinsAdapter.toggleViewHolder(position)
    }

    override fun onAddCoinClick() {
        viewModel.delegate.openManageCoins()
    }
}

class CoinsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(position: Int)
        fun onReceiveClicked(position: Int)
        fun onItemClick(position: Int)
        fun onAddCoinClick()
    }

    private val coinType = 1
    private val addCoinType = 2

    private var expandedViewPosition: Int? = null

    lateinit var viewDelegate: BalanceModule.IViewDelegate

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                addCoinType -> ViewHolderAddCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_add_coin, parent, false), listener)
                else -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false), listener)
            }

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder !is ViewHolderCoin) return

        if (payloads.isEmpty()) {
            holder.bind(viewDelegate.getViewItem(position), expandedViewPosition == position)
        } else if (payloads.any { it is Boolean }) {
            holder.bindPartial(expandedViewPosition == position)
        }
    }
}

class ViewHolderAddCoin(override val containerView: View, listener: CoinsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    init {
        manageCoins.setOnSingleClickListener {
            listener.onAddCoinClick()
        }
    }
}

class ViewHolderCoin(override val containerView: View, private val listener: CoinsAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var syncing = false

    fun bind(balanceViewItem: BalanceViewItem, expanded: Boolean) {
        syncing = false
        buttonPay.isEnabled = false
        imgSyncFailed.visibility = View.GONE
        iconProgress.visibility = View.GONE

        balanceViewItem.state.let { adapterState ->
            when (adapterState) {
                is AdapterState.Syncing -> {
                    syncing = true
                    iconProgress.visibility = View.VISIBLE
                    iconProgress.setProgress(adapterState.progress.toFloat())
                    adapterState.lastBlockDate?.let {
                        textSyncProgress.text = containerView.context.getString(R.string.Balance_SyncedUntil, DateHelper.formatDate(it, "MMM d.yyyy"))
                    }
                            ?: run { textSyncProgress.text = containerView.context.getString(R.string.Balance_Syncing) }
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

        balanceViewItem.currencyValue?.let {
            textCurrencyAmount.text = App.numberFormatter.format(it, trimmable = true)
            textCurrencyAmount.visibility = if (it.value.compareTo(BigDecimal.ZERO) == 0) View.GONE else View.VISIBLE
            textCurrencyAmount.alpha = if (balanceViewItem.rateExpired || syncing) 0.5f else 1f
        } ?: run { textCurrencyAmount.visibility = View.GONE }

        textCoinAmount.text = App.numberFormatter.format(balanceViewItem.coinValue)
        textCoinAmount.alpha = if (syncing) 0.5f else 1f

        textSyncProgress.visibility = if (expanded && syncing) View.VISIBLE else View.GONE
        textExchangeRate.visibility = if (expanded && syncing) View.GONE else View.VISIBLE

        coinIcon.bind(balanceViewItem.coin)
        textCoinName.text = balanceViewItem.coin.title

        textExchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            val rateString = App.numberFormatter.format(exchangeValue, trimmable = true, canUseLessSymbol = false)
            containerView.context.getString(R.string.Balance_RatePerCoin, rateString, balanceViewItem.coinValue.coinCode)
        } ?: ""
        textExchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.steel_40 else R.color.grey))

        buttonPay.setOnSingleClickListener {
            listener.onSendClicked(adapterPosition)
        }

        buttonReceive.setOnSingleClickListener {
            listener.onReceiveClicked(adapterPosition)
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            listener.onItemClick(adapterPosition)
        }
    }

    fun bindPartial(expanded: Boolean) {
        viewHolderRoot.isSelected = expanded
        textSyncProgress.visibility = if (expanded && syncing) View.VISIBLE else View.GONE
        textExchangeRate.visibility = if (expanded && syncing) View.GONE else View.VISIBLE
        if (expanded) {
            AnimationHelper.expand(buttonsWrapper)
        } else {
            AnimationHelper.collapse(buttonsWrapper)
        }

    }

}
