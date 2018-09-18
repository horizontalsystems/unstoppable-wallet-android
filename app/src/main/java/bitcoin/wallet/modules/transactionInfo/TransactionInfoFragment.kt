package bitcoin.wallet.modules.transactionInfo

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.HudHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper

class TransactionInfoFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: TransactionInfoViewModel

    private lateinit var transactionRecordViewItem: TransactionRecordViewItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)
        viewModel.init(transactionRecordViewItem)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = activity?.let { AlertDialog.Builder(it, R.style.BottomDialog) }

        val rootView = View.inflate(context, R.layout.fragment_bottom_sheet_transaction_info, null) as ViewGroup
        builder?.setView(rootView)

        mDialog = builder?.create()
        mDialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog?.window?.setGravity(Gravity.BOTTOM)

        rootView.findViewById<View>(R.id.txtClose)?.setOnClickListener { viewModel.delegate.onCloseClick() }
        rootView.findViewById<View>(R.id.transactionIdLayout)?.setOnClickListener { viewModel.delegate.onCopyId() }
        rootView.findViewById<View>(R.id.itemFromTo)?.setOnClickListener { viewModel.delegate.onCopyFromAddress() }
        rootView.findViewById<View>(R.id.itemStatus)?.setOnClickListener { viewModel.delegate.onStatusClick() }

        viewModel.transactionLiveData.observe(this, Observer { txRecord ->
            txRecord?.let { txRec ->

                rootView.findViewById<TextView>(R.id.txtAmount)?.apply {
                    val sign = if (txRec.incoming) "+" else "-"
                    text = "$sign ${NumberFormatHelper.cryptoAmountFormat.format(Math.abs(txRec.amount.value))} ${txRec.amount.coin.code}"
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                rootView.findViewById<TextView>(R.id.txDate)?.text = if (txRec.status == TransactionRecordViewItem.Status.PENDING) {
                    getString(R.string.transaction_info_processing)
                } else {
                    txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) }
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemStatus)?.apply {
                    val progressNumber = txRec.confirmations?.let { 100 / 6 * it.toInt() } ?: 0 //6 confirmations is accepted as 100% for transaction success
                    val valueIcon = when {
                        txRec.status == TransactionRecordViewItem.Status.PENDING -> R.drawable.pending
                        txRec.status == TransactionRecordViewItem.Status.PROCESSING -> null
                        else -> R.drawable.checkmark_green
                    }
                    val progress = when {
                        txRec.status == TransactionRecordViewItem.Status.PROCESSING -> progressNumber
                        else -> null
                    }
                    val statusText = when {
                        txRec.status == TransactionRecordViewItem.Status.PROCESSING -> getString(R.string.transaction_info_processing, progressNumber)
                        txRec.status == TransactionRecordViewItem.Status.PENDING -> getString(R.string.transaction_info_status_pending)
                        else -> getString(R.string.transaction_info_status_completed)
                    }
                    bind(title = getString(R.string.transaction_info_status), value = statusText, valueIcon = valueIcon, progressValue = progress)
                }

                rootView.findViewById<TextView>(R.id.transactionId)?.apply {
                    text = txRec.hash
                }

                rootView.findViewById<TextView>(R.id.fiatValue)?.apply {
                    text = "~\$${NumberFormatHelper.fiatAmountFormat.format(txRec.currencyAmount?.value)}"
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFromTo)?.apply {
                    val title = getString(if (txRec.incoming) R.string.transaction_info_from else R.string.transaction_info_to)
                    bind(title = title, value = txRec.from, valueIcon = R.drawable.round_person_18px)
                }
            }

        })

        viewModel.showDetailsLiveEvent.observe(this, Observer
        {
            //todo open Details activity
        })

        viewModel.closeLiveEvent.observe(this, Observer
        {
            dismiss()
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer
        {
            HudHelper.showSuccessMessage(R.string.hud_text_copied, activity)
        })

        return mDialog as Dialog
    }

    companion object {
        fun show(activity: FragmentActivity, transactionRecordViewItem: TransactionRecordViewItem) {
            val fragment = TransactionInfoFragment()
            fragment.transactionRecordViewItem = transactionRecordViewItem
            fragment.show(activity.supportFragmentManager, "receive_fragment")
        }
    }

}
