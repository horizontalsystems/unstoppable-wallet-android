package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.extensions.ConstraintLayoutWithHeader
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.view.*

class TransactionInfoView : ConstraintLayoutWithHeader {

    private lateinit var viewModel: TransactionInfoViewModel
    private lateinit var lifecycleOwner: LifecycleOwner
    private var listener: Listener? = null

    interface Listener {
        fun openTransactionInfo()
        fun openFullTransactionInfo(transactionHash: String, wallet: Wallet)
        fun closeTransactionInfo()
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(viewModel: TransactionInfoViewModel, lifecycleOwner: LifecycleOwner, listener: Listener) {
        setContentView(R.layout.transaction_info_bottom_sheet)

        this.viewModel = viewModel
        this.listener = listener
        this.lifecycleOwner = lifecycleOwner
        setTransactionInfoDialog()
    }

    private fun setTransactionInfoDialog() {
        setOnCloseCallback { listener?.closeTransactionInfo() }

        txtFullInfo.setOnClickListener { viewModel.onClickOpenFullInfo() }

        viewModel.showCopiedLiveEvent.observe(lifecycleOwner, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })

        viewModel.showFullInfoLiveEvent.observe(lifecycleOwner, Observer { pair ->
            pair?.let {
                listener?.openFullTransactionInfo(transactionHash = it.first, wallet = it.second)
            }
        })

        viewModel.transactionLiveData.observe(lifecycleOwner, Observer { txRecord ->
            txRecord?.let { txRec ->

                setTitle(context.getString(R.string.TransactionInfo_Title))
                setSubtitle(txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) })
                setHeaderIcon(LayoutHelper.getCoinDrawableResource(txRec.wallet.coin.code))

                itemId.apply {
                    bindHashId(context.getString(R.string.TransactionInfo_Id), txRec.transactionHash)
                    setOnClickListener { viewModel.onClickTransactionId() }
                }

                fiatValue.apply {
                    val fiatValueText = txRec.currencyValue?.let { App.numberFormatter.format(it, showNegativeSign = true, canUseLessSymbol = false) }
                    text = fiatValueText?.let { "$fiatValueText ${if (txRec.sentToSelf) "*" else ""}" }
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_d else R.color.yellow_d, null))
                }

                coinValue.text = App.numberFormatter.format(txRec.coinValue, explicitSign = true, realNumber = true)
                txInfoCoinName.text = txRec.wallet.coin.title

                if (txRec.rate == null) {
                    itemRate.visibility = View.GONE
                } else {
                    itemRate.visibility = View.VISIBLE
                    val rate = context.getString(R.string.Balance_RatePerCoin, App.numberFormatter.format(txRec.rate, canUseLessSymbol = false), txRec.wallet.coin.code)
                    itemRate.bind(context.getString(R.string.TransactionInfo_HistoricalRate), rate)
                }

                itemFee.visibility = View.GONE
                txRec.feeCoinValue?.let {
                    App.numberFormatter.format(txRec.feeCoinValue, explicitSign = false, realNumber = true)?.let { fee ->
                        itemFee.bind(context.getString(R.string.TransactionInfo_Fee), fee)
                        itemFee.visibility = View.VISIBLE
                    }
                }

                footNote.apply {
                    if (txRec.sentToSelf) {
                        val footNoteText = "* ${context.getString(R.string.TransactionInfo_FootNote)}"
                        text = footNoteText
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }

                }

                itemStatus.bindStatus(txRec.status)

                if (txRec.from.isNullOrEmpty() || !txRec.showFromAddress) {
                    itemFrom.visibility = View.GONE
                } else {
                    itemFrom.visibility = View.VISIBLE
                    itemFrom.setOnClickListener { viewModel.onClickFrom() }
                    itemFrom.bindAddress(context.getString(R.string.TransactionInfo_From), txRec.from)
                }

                if (txRec.to.isNullOrEmpty()) {
                    itemTo.visibility = View.GONE
                } else {
                    itemTo.visibility = View.VISIBLE
                    itemTo.setOnClickListener { viewModel.onClickTo() }
                    itemTo.bindAddress(context.getString(R.string.TransactionInfo_To), txRec.to)
                }

                listener?.openTransactionInfo()
            }
        })
    }

}
