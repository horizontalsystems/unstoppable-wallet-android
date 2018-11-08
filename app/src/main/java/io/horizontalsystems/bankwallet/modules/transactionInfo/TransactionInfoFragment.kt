package io.horizontalsystems.bankwallet.modules.transactionInfo

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionStatus
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter

class TransactionInfoFragment : DialogFragment() {

    private var mDialog: Dialog? = null

    private lateinit var viewModel: TransactionInfoViewModel

    private var transactionRecordViewItem: TransactionRecordViewItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transactionRecordViewItem?.let {
            viewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)
            viewModel.init(it)
        } ?: kotlin.run { dismiss() }
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
        rootView.findViewById<View>(R.id.itemFromTo)?.setOnClickListener { viewModel.delegate.onCopyAddress() }
//        rootView.findViewById<View>(R.id.itemStatus)?.setOnClickListener { viewModel.delegate.onStatusClick() }

        viewModel.transactionLiveData.observe(this, Observer { txRecord ->
            txRecord?.let { txRec ->
                val txStatus = txRec.status

                rootView.findViewById<TextView>(R.id.txtAmount)?.apply {
                    text = ValueFormatter.format(txRec.amount, true)
                    setTextColor(resources.getColor(if (txRec.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                rootView.findViewById<TextView>(R.id.txDate)?.text = if (txStatus is TransactionStatus.Pending) {
                    getString(R.string.transaction_info_status_pending)
                } else {
                    txRec.date?.let { DateHelper.getFullDateWithShortMonth(it) }
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemStatus)?.apply {
                    val valueIcon = when (txStatus) {
                        is TransactionStatus.Pending -> R.drawable.pending
                        is TransactionStatus.Processing -> null
                        else -> R.drawable.checkmark_green
                    }
                    val progress = when (txStatus) {
                        is TransactionStatus.Processing -> txStatus.progress
                        else -> null
                    }
                    val statusText = when (txStatus) {
                        is TransactionStatus.Processing -> getString(R.string.transaction_info_processing, progress)
                        is TransactionStatus.Pending -> getString(R.string.transaction_info_status_pending)
                        else -> getString(R.string.transaction_info_status_completed)
                    }
                    bind(title = getString(R.string.transaction_info_status), valueTitle = statusText.toUpperCase(), valueIcon = valueIcon, progressValue = progress)
                }

                rootView.findViewById<TextView>(R.id.transactionId)?.apply {
                    text = txRec.hash
                }

                rootView.findViewById<TextView>(R.id.fiatValue)?.apply {
                    text = txRec.currencyAmount?.let { ValueFormatter.format(it, true) }
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFromTo)?.apply {
                    val title = getString(if (txRec.incoming) R.string.transaction_info_from else R.string.transaction_info_to)
                    bind(title = title, valueTitle = if (txRec.incoming) txRec.from else txRec.to, valueIcon = R.drawable.round_person_18px)
                }
            }

        })

        viewModel.showDetailsLiveEvent.observe(this, Observer { pair ->
            pair?.let {
                val (adapterId, transactionId) = it
                activity?.let { activity ->
                    FullTransactionInfoModule.start(activity, adapterId, transactionId)
                }
            }
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
