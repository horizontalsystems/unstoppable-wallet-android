package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.view.*

class TransactionInfoView : ConstraintLayout {

    private lateinit var viewModel: TransactionInfoViewModel
    private lateinit var lifecycleOwner: LifecycleOwner
    private var listener: Listener? = null

    interface Listener {
        fun openTransactionInfo()
        fun openFullTransactionInfo(transactionHash: String, coin: Coin)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(viewModel: TransactionInfoViewModel, lifecycleOwner: LifecycleOwner, listener: Listener) {
        inflate(context, R.layout.transaction_info_bottom_sheet, this)
        this.viewModel = viewModel
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        setTransactionInfoDialog()
    }

    private fun setTransactionInfoDialog() {
        transactionIdView.setOnClickListener { viewModel.onClickTransactionId() }
        txtFullInfo.setOnClickListener { viewModel.onClickOpenFullInfo() }

        viewModel.showCopiedLiveEvent.observe(lifecycleOwner, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })

        viewModel.showFullInfoLiveEvent.observe(lifecycleOwner, Observer { pair ->
            pair?.let {
                listener?.openFullTransactionInfo(transactionHash = it.first, coin = it.second)
            }
        })

        viewModel.transactionLiveData.observe(lifecycleOwner, Observer { txRecord ->
            txRecord?.let { txRec ->
                val txStatus = txRec.status

                txInfoCoinIcon.bind(txRec.coin)

                fiatValue.apply {
                    text = txRec.currencyValue?.let { App.numberFormatter.format(it, showNegativeSign = true, canUseLessSymbol = false) }
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                coinValue.text = App.numberFormatter.format(txRec.coinValue, explicitSign = true, realNumber = true)
                txInfoCoinName.text = txRec.coin.title

                itemRate.apply {
                    txRec.rate?.let {
                        val rate = context.getString(R.string.Balance_RatePerCoin, App.numberFormatter.format(it, canUseLessSymbol = false), txRec.coin.code)
                        bind(title = context.getString(R.string.TransactionInfo_HistoricalRate), value = rate)
                    }
                    visibility = if (txRec.rate == null) View.GONE else View.VISIBLE
                }

                itemTime.bind(title = context.getString(R.string.TransactionInfo_Time), value = txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) }
                        ?: "")

                itemStatus.bindStatus(txStatus)

                transactionIdView.bindTransactionId(txRec.transactionHash)

                itemFrom.apply {
                    setOnClickListener { viewModel.onClickFrom() }
                    visibility = if (txRec.from.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = context.getString(R.string.TransactionInfo_From), address = txRec.from, showBottomBorder = true)
                }

                itemTo.apply {
                    setOnClickListener { viewModel.onClickTo() }
                    visibility = if (txRec.to.isNullOrEmpty()) View.GONE else View.VISIBLE
                    bindAddress(title = context.getString(R.string.TransactionInfo_To), address = txRec.to, showBottomBorder = true)
                }

                listener?.openTransactionInfo()
            }
        })
    }

}
