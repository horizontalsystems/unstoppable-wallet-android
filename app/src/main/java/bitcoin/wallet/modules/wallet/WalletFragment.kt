package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.core.setOnSingleClickListener
import bitcoin.wallet.modules.receive.ReceiveModule
import bitcoin.wallet.modules.send.SendModule
import bitcoin.wallet.viewHelpers.AnimationHelper
import bitcoin.wallet.viewHelpers.LayoutHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*


class WalletFragment : android.support.v4.app.Fragment(), CoinsAdapter.Listener {

    private lateinit var viewModel: WalletViewModel
    private var coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
        viewModel.init()

        viewModel.walletBalancesLiveData.observe(this, Observer { coins ->
            coins?.let {
                coinsAdapter.items = it
                coinsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.totalBalanceLiveData.observe(this, Observer { total ->
            val numberFormat = NumberFormatHelper.fiatAmountFormat
            ballanceText.text = total?.let { "${total.currency.symbol}${numberFormat.format(total.value)}" } ?: ""
        })

        viewModel.openReceiveDialog.observe(this, Observer { adapterId ->
            adapterId?.let{ id ->
                activity?.let {
                    ReceiveModule.start(it, id)
                }
            }
        })

        viewModel.openSendDialog.observe(this, Observer { iAdapter ->
            iAdapter?.let{ adapter ->
                activity?.let {
                    SendModule.start(it, adapter)
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(R.string.wallet_title)
        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = LinearLayoutManager(context)
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

    var items = listOf<WalletBalanceViewItem>()
    private var expandedViewPosition = -1

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        when (holder) {
            is ViewHolderCoin ->
                if (payloads.isEmpty()) {
                    holder.bind(items[position],
                            onSendClick = { listener.onSendClicked(items[position].adapterId) },
                            onReceiveClick = { listener.onReceiveClicked(items[position].adapterId) },
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

    fun bind(walletBalanceViewItem: WalletBalanceViewItem, onSendClick: (() -> (Unit))? = null, onReceiveClick: (() -> (Unit))? = null, onHolderClicked: (() -> Unit)? = null, expanded: Boolean) {
        val iconDrawable = ContextCompat.getDrawable(containerView.context, LayoutHelper.getCoinDrawableResource(walletBalanceViewItem.coinValue.coin.code))
        coinIcon.setImageDrawable(iconDrawable)
        val numberFormat = NumberFormatHelper.fiatAmountFormat
        textName.text = "${walletBalanceViewItem.coinValue.coin.name} (${walletBalanceViewItem.coinValue.coin.code})"
        textAmountFiat.text = "${walletBalanceViewItem.currencyValue.currency.symbol}${numberFormat.format(walletBalanceViewItem.currencyValue.value)}"
        textAmount.text = "${walletBalanceViewItem.coinValue.value}"

        val zeroBalance = walletBalanceViewItem.coinValue.value <= 0.0
        textAmount.visibility = if (zeroBalance) View.GONE else View.VISIBLE
        buttonPay.isEnabled = !zeroBalance
        textAmountFiat.isEnabled = !zeroBalance
        //todo convert indeterminate spinner to determinant one
        walletBalanceViewItem.progress?.subscribe{
            syncProgress.visibility = if (it == 1.0) View.GONE else View.VISIBLE
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

}
