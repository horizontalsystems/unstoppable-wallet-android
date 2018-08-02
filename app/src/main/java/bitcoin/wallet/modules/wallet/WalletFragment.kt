package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import bitcoin.wallet.R
import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.modules.receive.ReceiveModule
import bitcoin.wallet.modules.send.SendModule
import bitcoin.wallet.viewHelpers.AnimationHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*

class WalletFragment : android.support.v4.app.Fragment(), CoinsAdapter.Listener {

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
                tmpItems.add(WalletBalanceViewItem(CoinValue(Ethereum(), 0.0), CurrencyValue(DollarCurrency(), 750.0), CurrencyValue(DollarCurrency(), 0.0), false))
                //todo end
                coinsAdapter.items = tmpItems//it //todo replace tmpItems with it
                coinsAdapter.notifyDataSetChanged()
            }
        })

        viewModel.totalBalanceLiveData.observe(this, Observer { total ->
            val numberFormat = NumberFormatHelper.fiatAmountFormat
            ballanceText.text = total?.let { "${total.currency.symbol}${numberFormat.format(total.value)}" } ?: ""
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setTitle(R.string.tab_title_wallet)
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

    var items = listOf<WalletBalanceViewItem>()
    private var expandedViewPosition = -1

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolderCoin(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_coin, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderCoin -> holder.bind(items[position],
                    onPayClick = { listener.onPayClicked(items[position].coinValue.coin) },
                    onReceiveClick = { listener.onReceiveClicked(items[position].coinValue.coin) },
                    onHolderCLicked = {
                        expandedViewPosition = if (expandedViewPosition == position) -1 else position
                        notifyDataSetChanged()
                    },
                    expand = expandedViewPosition == position)
        }
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
        syncProgress.visibility = if (walletBalanceViewItem.syncing) View.VISIBLE else View.GONE

        buttonPay.setOnClickListener {
            onPayClick?.invoke()
        }

        buttonReceive.setOnClickListener {
            onReceiveClick?.invoke()
        }

        containerView.setOnClickListener {
            onHolderCLicked?.invoke()
        }

        if (expand && buttonsWrapper.visibility == View.GONE) {
            AnimationHelper.expand(buttonsWrapper)
        } else if(!expand && buttonsWrapper.visibility == View.VISIBLE) {
            AnimationHelper.collapse(buttonsWrapper)
        }

    }

}
