package io.horizontalsystems.bankwallet.modules.balance

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.managecoins.ManageCoinsModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*


class BalanceFragment : android.support.v4.app.Fragment(), CoinsAdapter.Listener {

    private lateinit var viewModel: BalanceViewModel
    private var coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        toolbar.setTitle(R.string.Balance_Title)

        viewModel = ViewModelProviders.of(this).get(BalanceViewModel::class.java)
        viewModel.init()


        viewModel.totalBalanceLiveData.observe(this, Observer { total ->
            ballanceText.text = total?.let { ValueFormatter.format(it) } ?: ""
        })

        viewModel.openReceiveDialog.observe(this, Observer { adapterId ->
            adapterId?.let { id ->
                activity?.let {
                    ReceiveModule.start(it, id)
                }
            }
        })

        viewModel.openSendDialog.observe(this, Observer { iAdapter ->
            iAdapter?.let { coin ->
                activity?.let {
                    SendModule.start(it, coin)
                }
            }
        })

        viewModel.balanceColorLiveDate.observe(this, Observer { color ->
            color?.let { colorRes ->
                context?.let { it ->
                    ballanceText.setTextColor(ContextCompat.getColor(it, colorRes))
                }
            }
        })

        viewModel.didRefreshLiveEvent.observe(this, Observer {
            pullToRefresh.isRefreshing = false
        })

        viewModel.openManageCoinsLiveEvent.observe(this, Observer {
            context?.let { context -> ManageCoinsModule.start(context) }
        })

        viewModel.reloadLiveEvent.observe(this, Observer {
            coinsAdapter.notifyDataSetChanged()
            reloadHeader()
        })

        viewModel.reloadHeaderLiveEvent.observe(this, Observer {
            reloadHeader()
        })

        viewModel.reloadItemLiveEvent.observe(this, Observer { position ->
            position?.let {
                coinsAdapter.notifyItemChanged(it)
            }
        })


        coinsAdapter.viewDelegate = viewModel.delegate
        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = LinearLayoutManager(context)
        (recyclerCoins.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        manageCoins.setOnSingleClickListener { viewModel.delegate.openManageCoins() }

        pullToRefresh.setOnRefreshListener {
            viewModel.delegate.refresh()
        }
    }

    private fun reloadHeader() {
        val headerViewItem = viewModel.delegate.getHeaderViewItem()

        context?.let {
            val color = if (headerViewItem.upToDate) R.color.yellow_crypto else R.color.yellow_crypto_40
            ballanceText.setTextColor(ContextCompat.getColor(it, color))
        }

        ballanceText.text = headerViewItem.currencyValue?.let {
            ValueFormatter.format(it)
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
        recyclerCoins.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
    }
}

class CoinsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(position: Int)
        fun onReceiveClicked(position: Int)
        fun onItemClick(position: Int)
    }

    private var expandedViewPosition = -1

    lateinit var viewDelegate: BalanceModule.IViewDelegate

    override fun getItemCount() = viewDelegate.itemsCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        when (holder) {
            is ViewHolderCoin -> holder.unbind()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (holder) {
            is ViewHolderCoin ->
                if (payloads.isEmpty()) {
                    val balanceViewItem = viewDelegate.getViewItem(position)
                    holder.bind(balanceViewItem,
                            onSendClick = { listener.onSendClicked(position) },
                            onReceiveClick = { listener.onReceiveClicked(position) },
                            onHolderClicked = {
                                val oldExpandedViewPosition = expandedViewPosition
                                expandedViewPosition = if (expandedViewPosition == position) -1 else position
                                notifyItemChanged(expandedViewPosition, true)
                                if (oldExpandedViewPosition != -1) {
                                    notifyItemChanged(oldExpandedViewPosition, false)
                                }
                            },
                            expanded = expandedViewPosition == position)
                } else {
                    for (payload in payloads) {
                        if (payload is Boolean) {
                            holder.bindPartial(expanded = expandedViewPosition == position)
                        }
                    }
                }
        }
    }
}

class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var disposable: Disposable? = null

    fun bind(balanceViewItem: BalanceViewItem, onSendClick: (() -> (Unit))? = null, onReceiveClick: (() -> (Unit))? = null, onHolderClicked: (() -> Unit)? = null, expanded: Boolean) {
        buttonPay.isEnabled = false
        imgSyncFailed.visibility = View.GONE
        textCurrencyAmount.visibility = View.GONE
        textCoinAmount.visibility = View.GONE
        progressSync.visibility = View.GONE
        textSyncProgress.visibility = View.GONE

        balanceViewItem.state.let { adapterState ->
            when (adapterState) {
                is AdapterState.Syncing -> {
                    progressSync.visibility = View.VISIBLE
                    textSyncProgress.visibility = View.VISIBLE

                    disposable = adapterState.progressSubject
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                val progress = (it * 100).toInt()
                                textSyncProgress.text = "$progress%"
                            }
                }
                is AdapterState.Synced -> {
                    if (balanceViewItem.coinValue.value > 0) {
                        textCurrencyAmount.visibility = View.VISIBLE
                        textCurrencyAmount.text = balanceViewItem.currencyValue?.let { ValueFormatter.format(it) }
                        textCurrencyAmount.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.yellow_crypto_40 else R.color.yellow_crypto))
                        buttonPay.isEnabled = true
                    }

                    textCoinAmount.visibility = View.VISIBLE
                    textCoinAmount.text = ValueFormatter.format(balanceViewItem.coinValue)
                }
                is AdapterState.NotSynced -> {
                    imgSyncFailed.visibility = View.VISIBLE
                }
            }
        }

        val iconDrawable = ContextCompat.getDrawable(containerView.context, LayoutHelper.getCoinDrawableResource(TextHelper.getCleanCoinCode(balanceViewItem.coinValue.coinCode)))
        imgCoin.setImageDrawable(iconDrawable)

        textCoinName.text = balanceViewItem.coinValue.coinCode

        textExchangeRate.text = balanceViewItem.exchangeValue?.let { exchangeValue ->
            containerView.context.getString(R.string.Balance_RatePerCoin, ValueFormatter.format(exchangeValue), balanceViewItem.coinValue.coinCode)
        } ?: ""
        textExchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (balanceViewItem.rateExpired) R.color.steel_40 else R.color.grey))

        buttonPay.setOnSingleClickListener {
            onSendClick?.invoke()
        }

        buttonReceive.setOnSingleClickListener {
            onReceiveClick?.invoke()
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnClickListener {
            onHolderClicked?.invoke()
        }
    }

    fun bindPartial(expanded: Boolean) {
        viewHolderRoot.isSelected = expanded
        if (expanded) {
            AnimationHelper.expand(buttonsWrapper)
        } else {
            AnimationHelper.collapse(buttonsWrapper)
        }

    }

    fun unbind() {
        disposable?.dispose()
    }

}
