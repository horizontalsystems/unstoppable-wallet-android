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
            if (txRecord != null) {

                rootView.findViewById<TextView>(R.id.txtAmount)?.apply {
                    val sign = if (txRecord.incoming) "+" else "-"
                    text = "$sign ${NumberFormatHelper.cryptoAmountFormat.format(Math.abs(txRecord.amount.value))} ${txRecord.amount.coin.code}"
                    setTextColor(resources.getColor(if (txRecord.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                rootView.findViewById<TextView>(R.id.txDate)?.text = if (txRecord.status == TransactionRecordViewItem.Status.PENDING) {
                    getString(R.string.tx_info_bottom_sheet_status_processing)
                } else {
                    txRecord.date?.let { DateHelper.getFullDateWithShortMonth(it) }
                }

                val statusText = when {
                    txRecord.status == TransactionRecordViewItem.Status.PROCESSING -> R.string.tx_info_bottom_sheet_status_processing
                    txRecord.status == TransactionRecordViewItem.Status.PENDING -> R.string.tx_info_bottom_sheet_status_pending
                    else -> R.string.tx_info_bottom_sheet_title_completed
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemStatus)?.apply {
                    value = getString(statusText).toUpperCase()
                    showValueBackground = true
                    valueIcon = if (txRecord.status == TransactionRecordViewItem.Status.SUCCESS) R.drawable.checkmark_green else R.drawable.pending
                }

                rootView.findViewById<TextView>(R.id.transactionId)?.apply {
                    text = txRecord.hash
                }

                rootView.findViewById<TextView>(R.id.fiatValue)?.apply {
                    text = "~\$${NumberFormatHelper.fiatAmountFormat.format(txRecord.currencyAmount?.value)}"
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFromTo)?.apply {
                    title = getString(if (txRecord.incoming) R.string.tx_info_bottom_sheet_from else R.string.tx_info_bottom_sheet_to)
                    value = txRecord.from
                    showValueBackground = true
                    valueIcon = R.drawable.round_person_18px
                }

            }

        })

        viewModel.showDetailsLiveEvent.observe(this, Observer {
            //todo open Details activity
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            dismiss()
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
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
