package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.modules.main.BaseTabFragment
import bitcoin.wallet.modules.send.SendModule
import bitcoin.wallet.modules.receive.ReceiveModule
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*

class WalletFragment : BaseTabFragment(), CoinsAdapter.Listener {

    override val title: Int
        get() = R.string.tab_title_wallet

    private lateinit var viewModel: WalletViewModel
    private val coinsAdapter = CoinsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(WalletViewModel::class.java)
        viewModel.init()

        viewModel.walletBalancesLiveData.observe(this, Observer { coins ->
            coins?.let {

                //todo begin - for testing purposes, remove after testing
                val tmpItems = it.toMutableList()
                tmpItems.add(WalletBalanceViewItem(CoinValue(Ethereum(), 0.0), CurrencyValue(DollarCurrency(), 750.0), CurrencyValue(DollarCurrency(), 0.0)))
                //todo end
                coinsAdapter.items = tmpItems//it //todo replace tmpItems with it
                coinsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.totalBalanceLiveData.observe(this, Observer { total ->
            total?.let {
                coinsAdapter.total = it
                coinsAdapter.notifyDataSetChanged()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerCoins.adapter = coinsAdapter
        recyclerCoins.layoutManager = LinearLayoutManager(context)
    }

    override fun onPayClicked(coin: Coin) {
        activity?.let {
            SendModule.start(it, coin)
        }
    }

    override fun onReceiveClicked(coin: Coin) {
        activity?.let {
            ReceiveModule.start(it, coin.code)
        }
    }
}

class CoinsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onPayClicked(coin: Coin)
        fun onReceiveClicked(coin: Coin)
    }

    companion object {
        const val viewTypeTotal = 1
        const val viewTypeCoin = 2

    }

    var items = listOf<WalletBalanceViewItem>()
    var total: CurrencyValue? = null
    private var expandedViewPosition = -1

    override fun getItemCount() = items.size + 1

    override fun getItemViewType(position: Int) = when (position) {
        0 -> viewTypeTotal
        else -> viewTypeCoin
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        viewTypeCoin -> ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false))
        else -> ViewHolderTotalBalance(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_total, parent, false) as TextView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderCoin -> holder.bind(items[position - 1],
                    onPayClick = { listener.onPayClicked(items[position - 1].coinValue.coin) },
                    onReceiveClick = { listener.onReceiveClicked(items[position - 1].coinValue.coin) },
                    onHolderCLicked = {
                        val oldExpandedViewPosition = expandedViewPosition
                        expandedViewPosition = if (expandedViewPosition == position) -1 else position
                        if (oldExpandedViewPosition != -1) {
                            notifyItemChanged(oldExpandedViewPosition)
                        }
                        notifyItemChanged(expandedViewPosition)
                    },
                    expand = expandedViewPosition == position)
            is ViewHolderTotalBalance -> holder.bind(total)
        }
    }

}

class ViewHolderTotalBalance(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
    fun bind(total: CurrencyValue?) {
        val numberFormat = NumberFormatHelper.fiatAmountFormat
        textView.text = total?.let { "${total.currency.symbol}${numberFormat.format(total.value)}" } ?: ""
    }
}

class ViewHolderCoin(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(walletBalanceViewItem: WalletBalanceViewItem, onPayClick: (() -> (Unit))? = null, onReceiveClick: (() -> (Unit))? = null, onHolderCLicked: (() -> (Unit))? = null, expand: Boolean = false) {
        val numberFormat = NumberFormatHelper.fiatAmountFormat
        textName.text = "${walletBalanceViewItem.coinValue.coin.name} (${walletBalanceViewItem.coinValue.coin.code})"
        textAmountFiat.text = "${walletBalanceViewItem.currencyValue.currency.symbol}${numberFormat.format(walletBalanceViewItem.currencyValue.value)}"
        textAmount.text = "${walletBalanceViewItem.coinValue.value}"

        val zeroBalance = walletBalanceViewItem.coinValue.value <= 0.0
        textAmount.visibility = if (zeroBalance) View.GONE else View.VISIBLE
        buttonPay.isEnabled = !zeroBalance
        textAmountFiat.isEnabled = !zeroBalance

        buttonPay.setOnClickListener {
            onPayClick?.invoke()
        }

        buttonReceive.setOnClickListener {
            onReceiveClick?.invoke()
        }

        containerView.setOnClickListener {
            onHolderCLicked?.invoke()
        }

        buttonReceive.visibility = if (expand) View.VISIBLE else View.GONE
        buttonPay.visibility = if (expand) View.VISIBLE else View.GONE
    }

}
