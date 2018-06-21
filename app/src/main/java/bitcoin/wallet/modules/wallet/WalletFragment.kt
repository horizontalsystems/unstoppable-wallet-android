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
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.WalletBalanceViewItem
import bitcoin.wallet.modules.main.BaseTabFragment
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.android.synthetic.main.view_holder_coin.*

class WalletFragment : BaseTabFragment() {

    override val title: Int
        get() = R.string.tab_title_wallet

    private lateinit var viewModel: WalletViewModel
    private val coinsAdapter = CoinsAdapter()

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
}

class CoinsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val viewTypeTotal = 1
        const val viewTypeCoin = 2

    }

    var items = listOf<WalletBalanceViewItem>()
    var total: CurrencyValue? = null

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
            is ViewHolderCoin -> holder.bind(items[position - 1])
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

    private var buttonsVisible = false

    init {
        toggleButtons()

        containerView.setOnClickListener {
            toggleButtons()
        }
    }

    fun bind(walletBalanceViewItem: WalletBalanceViewItem) {
        val numberFormat = NumberFormatHelper.fiatAmountFormat
        textName.text = walletBalanceViewItem.coinValue.coin.name
        textRate.text = "${walletBalanceViewItem.exchangeValue.currency.symbol}${numberFormat.format(walletBalanceViewItem.exchangeValue.value)}"
        textAmountFiat.text = "${walletBalanceViewItem.currencyValue.currency.symbol}${numberFormat.format(walletBalanceViewItem.currencyValue.value)}"
        textAmount.text = "${walletBalanceViewItem.coinValue.value} ${walletBalanceViewItem.coinValue.coin.code}"
    }

    private fun toggleButtons() {
        buttonReceive.visibility = if (buttonsVisible) View.VISIBLE else View.GONE
        buttonPay.visibility = if (buttonsVisible) View.VISIBLE else View.GONE

        buttonsVisible = !buttonsVisible
    }

}
