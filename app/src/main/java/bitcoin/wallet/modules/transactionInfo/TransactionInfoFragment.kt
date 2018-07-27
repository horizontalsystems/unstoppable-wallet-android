package bitcoin.wallet.modules.transactionInfo

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper

class TransactionInfoFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: TransactionInfoViewModel

    private lateinit var coinCode: String
    private lateinit var txHash: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)
        viewModel.init(coinCode, txHash)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_transaction_info, null) as ViewGroup
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<View>(R.id.lessMoreLayout)?.setOnClickListener { viewModel.delegate.onLessMoreClick() }

        viewModel.transactionLiveData.observe(this, Observer { txRecord ->
            if (txRecord != null) {

                rootView.findViewById<TextView>(R.id.txtTitle)?.setText(if (txRecord.incoming) R.string.tx_info_bottom_sheet_title_received else R.string.tx_info_bottom_sheet_title_sent)

                rootView.findViewById<TextView>(R.id.txtAmount)?.apply {
                    text = "${NumberFormatHelper.cryptoAmountFormat.format(txRecord.amount.value)} ${txRecord.amount.coin.code} "
                    setTextColor(resources.getColor(if (txRecord.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                val valueInBaseCurrency = "$${NumberFormatHelper.fiatAmountFormat.format(txRecord.valueInBaseCurrency)}"

                rootView.findViewById<TextView>(R.id.txtValueOnTxDate)?.text = if (txRecord.incoming) {
                    getString(R.string.tx_info_bottom_sheet_value_when_received, valueInBaseCurrency)
                } else {
                    getString(R.string.tx_info_bottom_sheet_value_when_sent, valueInBaseCurrency)
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemStatus)?.apply {
                    title = if (txRecord.status == TransactionRecordViewItem.Status.SUCCESS)
                        getString(R.string.tx_info_bottom_sheet_status_success)
                    else
                        getString(R.string.tx_info_bottom_sheet_status_pending)
                    value = DateHelper.getFullDateWithTime(txRecord.date)
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFrom)?.value = txRecord.from

                rootView.findViewById<TransactionInfoItemView>(R.id.itemReference)?.value = "" //todo tx reference should be set

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFee)?.value = "${NumberFormatHelper.cryptoAmountFormat.format(txRecord.fee.value)} ${txRecord.amount.coin.code}"

                rootView.findViewById<TransactionInfoItemView>(R.id.itemExchangeRate)?.value = "$${NumberFormatHelper.fiatAmountFormat.format(txRecord.exchangeRate)}"

                rootView.findViewById<TransactionInfoItemView>(R.id.itemConfirmedBlock)?.value = txRecord.blockHeight.toString()

                rootView.findViewById<TransactionInfoItemView>(R.id.itemTransactionId)?.value = abbreviateMiddle(txRecord.hash, "...", 24)
            }

        })

        viewModel.expandLiveEvent.observe(this, Observer { expand ->
            rootView.findViewById<View>(R.id.moreItemsLayout).visibility = if (expand == true) View.VISIBLE else View.GONE
            rootView.findViewById<TextView>(R.id.txtExpand).setText(if (expand == true) R.string.tx_info_bottom_sheet_less else R.string.tx_info_bottom_sheet_more)
        })

        return mDialog as Dialog
    }

    private fun abbreviateMiddle(str: String, middle: String, length: Int): String {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(middle)) {
            return str
        }

        if (length >= str.length || length < middle.length + 2) {
            return str
        }

        val targetSting = length - middle.length
        val startOffset = targetSting / 2 + targetSting % 2
        val endOffset = str.length - targetSting / 2

        return str.substring(0, startOffset) +
                middle +
                str.substring(endOffset)
    }

    companion object {
        fun show(activity: FragmentActivity, coinCode: String, txHash: String) {
            val fragment = TransactionInfoFragment()
            fragment.coinCode = coinCode
            fragment.txHash = txHash
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
