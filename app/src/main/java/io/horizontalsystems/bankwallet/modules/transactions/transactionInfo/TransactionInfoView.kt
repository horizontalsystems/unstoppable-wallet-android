package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.info.InfoModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.ui.extensions.ConstraintLayoutWithHeader
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
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
        btnShare.setOnClickListener{ viewModel.onClickShareId() }

        viewModel.showCopiedLiveEvent.observe(lifecycleOwner, Observer {
            HudHelper.showSuccessMessage(context, R.string.Hud_Text_Copied)
        })

        viewModel.showShareLiveEvent.observe(lifecycleOwner, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            context.startActivity(sendIntent)
        })


        viewModel.showFullInfoLiveEvent.observe(lifecycleOwner, Observer { pair ->
            pair?.let {
                listener?.openFullTransactionInfo(transactionHash = it.first, wallet = it.second)
            }
        })

        viewModel.showLockInfo.observe(lifecycleOwner, Observer { lockDate ->
            val title = context.getString(R.string.Info_LockTime_Title)
            val description = context.getString(R.string.Info_LockTime_Description, DateHelper.getFullDate(lockDate))

            InfoModule.start(context, InfoModule.InfoParameters(title, description))
        })

        viewModel.showDoubleSpendInfo.observe(lifecycleOwner, Observer { (txHash, conflictingTxHash) ->
            val title = context.getString(R.string.Info_DoubleSpend_Title)
            val description = context.getString(R.string.Info_DoubleSpend_Description)

            InfoModule.start(context, InfoModule.InfoParameters(title, description, txHash, conflictingTxHash))
        })

        viewModel.transactionLiveData.observe(lifecycleOwner, Observer { txRecord ->
            txRecord?.let { txRec ->

                val incoming = txRec.type == TransactionType.Incoming
                val sentToSelf = txRec.type == TransactionType.SentToSelf

                setTitle(context.getString(R.string.TransactionInfo_Title))
                setSubtitle(txRec.date?.let { DateHelper.getFullDate(it) })
                setHeaderIcon(if (incoming) R.drawable.ic_incoming else R.drawable.ic_outgoing)

                itemId.apply {
                    bindHashId(context.getString(R.string.TransactionInfo_Id), txRec.transactionHash)
                    setOnClickListener { viewModel.onClickTransactionId() }
                }

                val currencyValue = txRec.currencyValue
                if (currencyValue != null) {
                    fiatValueWrapper.visibility = View.VISIBLE
                    fiatName.visibility = View.VISIBLE

                    fiatValue.text =  App.numberFormatter.formatForTransactions(context, currencyValue, incoming, canUseLessSymbol = false, trimmable = false)

                    val lockIcon = when {
                        txRec.lockInfo == null -> 0
                        txRec.unlocked -> R.drawable.ic_unlock
                        else -> R.drawable.ic_lock
                    }
                    fiatValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, lockIcon, 0)
                    fiatName.text = currencyValue.currency.code
                    sentToSelfIcon.visibility = if (sentToSelf) View.VISIBLE else View.GONE
                } else {
                    fiatValueWrapper.visibility = View.GONE
                    fiatName.visibility = View.GONE
                }

                coinValue.text = App.numberFormatter.format(txRec.coinValue, explicitSign = true, realNumber = true)
                txInfoCoinName.text = txRec.wallet.coin.title

                if (txRec.lockInfo != null) {
                    itemLockTime.visibility = View.VISIBLE
                    itemLockTime.bindInfo("${context.getString(R.string.TransactionInfo_LockedUntil)} ${DateHelper.getFullDate(txRec.lockInfo.lockedUntil)}", R.drawable.ic_lock)
                    itemLockTime.setOnClickListener { viewModel.onClickLockInfo() }

                } else {
                    itemLockTime.visibility = View.GONE
                }

                if (txRec.conflictingTxHash != null) {
                    itemDoubleSpend.visibility = View.VISIBLE
                    itemDoubleSpend.bindInfo(context.getString(R.string.TransactionInfo_DoubleSpendNote), R.drawable.ic_doublespend)
                    itemDoubleSpend.setOnClickListener { viewModel.onClickDoubleSpendInfo() }
                } else {
                    itemDoubleSpend.visibility = View.GONE
                }

                val rate = txRec.rate
                if (rate == null) {
                    itemRate.visibility = View.GONE
                } else {
                    itemRate.visibility = View.VISIBLE
                    val rateValue = context.getString(R.string.Balance_RatePerCoin, App.numberFormatter.formatForRates(rate), txRec.wallet.coin.code)
                    itemRate.bind(context.getString(R.string.TransactionInfo_HistoricalRate), rateValue)
                }

                itemFee.visibility = View.GONE
                txRec.feeCoinValue?.let {feeCoinValue ->
                    getFeeText(feeCoinValue, txRec)?.let{ fee->
                        itemFee.bind(title = context.getString(R.string.TransactionInfo_Fee), value = fee)
                        itemFee.visibility = View.VISIBLE
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

                if (sentToSelf) {
                    itemSentToSelf.bindSentToSelfNote()
                    itemSentToSelf.visibility = View.VISIBLE
                } else {
                    itemSentToSelf.visibility = View.GONE
                }

                listener?.openTransactionInfo()
            }
        })
    }

    private fun getFeeText(feeCoinValue: CoinValue, txRec: TransactionViewItem): String? {
        val coinFee = App.numberFormatter.format(feeCoinValue, explicitSign = false, realNumber = true) ?: return null
        val rate = txRec.rate ?: return coinFee
        val fiatFee = App.numberFormatter.format(CurrencyValue(rate.currency, value = rate.value.times(feeCoinValue.value))) ?: return coinFee
        return "$coinFee | $fiatFee"
    }

}
