package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.info.InfoModule
import io.horizontalsystems.bankwallet.ui.extensions.ConstraintLayoutWithHeader
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
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

        viewModel.showLockInfo.observe(lifecycleOwner, Observer { lockDate ->
            val title = context.getString(R.string.Info_LockTime_Title)
            val description = context.getString(R.string.Info_LockTime_Description, DateHelper.formatDate(lockDate, "MMM dd, yyyy"))

            InfoModule.start(context, InfoModule.InfoParameters(title, description))
        })

        viewModel.transactionLiveData.observe(lifecycleOwner, Observer { txRecord ->
            txRecord?.let { txRec ->

                val incoming = txRec.type == TransactionType.Incoming
                val sentToSelf = txRec.type == TransactionType.SentToSelf

                setTitle(context.getString(R.string.TransactionInfo_Title))
                setSubtitle(txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) })
                setHeaderIcon(if (incoming) R.drawable.ic_incoming else R.drawable.ic_outgoing)

                itemId.apply {
                    bindHashId(context.getString(R.string.TransactionInfo_Id), txRec.transactionHash)
                    setOnClickListener { viewModel.onClickTransactionId() }
                }

                if (txRec.currencyValue != null) {
                    fiatValue.visibility = View.VISIBLE
                    fiatName.visibility = View.VISIBLE

                    val fiatValueText = App.numberFormatter.format(txRec.currencyValue, showNegativeSign = false, canUseLessSymbol = false)
                    fiatValue.text = fiatValueText?.let { "$fiatValueText${if (sentToSelf) " *" else ""}" }
                    fiatValue.setTextColor(resources.getColor(if (incoming) R.color.green_d else R.color.yellow_d, null))
                    fiatValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, if (txRec.lockInfo != null) R.drawable.ic_lock else 0, 0)
                    fiatName.text = txRec.currencyValue.currency.code
                } else {
                    fiatValue.visibility = View.GONE
                    fiatName.visibility = View.GONE
                }

                coinValue.text = App.numberFormatter.format(txRec.coinValue, explicitSign = true, realNumber = true)
                txInfoCoinName.text = txRec.wallet.coin.title

                if (txRec.lockInfo != null) {
                    itemLockTime.visibility = View.VISIBLE
                    itemLockTime.bindInfo(context.getString(R.string.TransactionInfo_LockedUntil), DateHelper.formatDate(txRec.lockInfo.lockedUntil, "MMM dd, yyyy, h a"))
                    itemLockTime.setOnClickListener { viewModel.onClickLockInfo() }
                } else {
                    itemLockTime.visibility = View.GONE
                }

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
                    if (sentToSelf && txRec.lockInfo == null) {
                        val footNoteText = "* ${context.getString(R.string.TransactionInfo_FootNote)}"
                        text = footNoteText
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }

                }

                itemStatus.bindStatus(txRec.status, incoming)

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

                if (incoming || txRec.lockInfo == null) {
                    itemRecipientHash.visibility = View.GONE
                } else {
                    itemRecipientHash.visibility = View.VISIBLE
                    itemRecipientHash.setOnClickListener { viewModel.onClickRecipientHash() }
                    itemRecipientHash.bindAddress(context.getString(R.string.TransactionInfo_RecipientHash), txRec.lockInfo.originalAddress)
                }

                listener?.openTransactionInfo()
            }
        })
    }

}
