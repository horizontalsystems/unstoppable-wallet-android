package io.horizontalsystems.bankwallet.modules.wallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.receive.ReceiveModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.viewHelpers.AnimationHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import io.reactivex.disposables.Disposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*


class WalletFragment : android.support.v4.app.Fragment(), CoinsAdapter.Listener {

    private lateinit var viewModel: WalletViewModel
    private var coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
        viewModel.init()

        viewModel.titleLiveDate.observe(this, Observer { title ->
            title?.let { toolbar.setTitle(it) }
        })

        viewModel.walletsLiveData.observe(this, Observer { coins ->
            coins?.let {
                coinsAdapter.items = it
                coinsAdapter.notifyDataSetChanged()
            }
        })

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

        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = LinearLayoutManager(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerCoins.adapter = null
    }

    override fun onSendClicked(adapterId: String) {
        viewModel.onSendClicked(adapterId)
    }

    override fun onReceiveClicked(adapterId: String) {
        viewModel.onReceiveClicked(adapterId)
    }

    override fun onItemClick(position: Int) {
        recyclerCoins.findViewHolderForAdapterPosition(position)?.itemView?.performClick()
    }
}

class CoinsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onSendClicked(adapterId: String)
        fun onReceiveClicked(adapterId: String)
        fun onItemClick(position: Int)
    }

    var items = listOf<WalletViewItem>()
    private var expandedViewPosition = -1

    override fun getItemCount() = items.size

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
                    holder.bind(items[position],
                            onSendClick = { listener.onSendClicked(items[position].coinValue.coin) },
                            onReceiveClick = { listener.onReceiveClicked(items[position].coinValue.coin) },
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

    fun bind(walletViewItem: WalletViewItem, onSendClick: (() -> (Unit))? = null, onReceiveClick: (() -> (Unit))? = null, onHolderClicked: (() -> Unit)? = null, expanded: Boolean) {
        val iconDrawable = ContextCompat.getDrawable(containerView.context, LayoutHelper.getCoinDrawableResource(TextHelper.getCleanCoinCode(walletViewItem.coinValue.coin)))
        coinIcon.setImageDrawable(iconDrawable)
        textName.text = "${walletViewItem.coinValue.coin}"
        textAmountFiat.text = walletViewItem.currencyValue?.let { ValueFormatter.format(it) }
        coinAmount.text = "${ValueFormatter.format(walletViewItem.coinValue)}"

        buttonPay.isEnabled = (walletViewItem.state is AdapterState.Synced)
        textExchangeRate.text = walletViewItem.exchangeValue?.let { exchangeValue ->
            containerView.context.getString(R.string.wallet_exchange_rate, ValueFormatter.format(exchangeValue), walletViewItem.coinValue.coin)
        } ?: kotlin.run { "" }

        textExchangeRate.setTextColor(ContextCompat.getColor(containerView.context, if (walletViewItem.rateExpired) R.color.grey_40 else R.color.grey))
        textAmountFiat.setTextColor(ContextCompat.getColor(containerView.context, if (walletViewItem.rateExpired) R.color.yellow_crypto_40 else R.color.yellow_crypto))

        val isSyncing = walletViewItem.state is AdapterState.Syncing
        val zeroBalance = walletViewItem.coinValue.value <= 0.0

        syncProgress.visibility = if(isSyncing) View.VISIBLE else View.GONE
        coinSyncProgress.visibility = if(isSyncing) View.VISIBLE else View.GONE
        textAmountFiat.visibility = if(isSyncing || zeroBalance) View.GONE else View.VISIBLE
        coinAmount.visibility = if (isSyncing || zeroBalance) View.GONE else View.VISIBLE

        if (isSyncing) {
            disposable = walletViewItem.state.progressSubject?.subscribe {
                val progress = (it * 100).toInt()
                coinSyncProgress.text = "$progress%"
            }
        }

        buttonPay.setOnClickListener {
            onSendClick?.invoke()
        }

        buttonReceive.setOnClickListener {
            onReceiveClick?.invoke()
        }

        viewHolderRoot.isSelected = expanded
        buttonsWrapper.visibility = if (expanded) View.VISIBLE else View.GONE
        containerView.setOnSingleClickListener {
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
