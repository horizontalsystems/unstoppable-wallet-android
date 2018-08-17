package bitcoin.wallet.modules.transactionInfo

import android.app.AlertDialog
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import bitcoin.wallet.R
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.LayoutHelper
import bitcoin.wallet.viewHelpers.NumberFormatHelper
import bitcoin.wallet.viewHelpers.TextHelper

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

        rootView.findViewById<View>(R.id.detailsBtn)?.setOnClickListener { viewModel.delegate.onDetailsClick() }
        rootView.findViewById<View>(R.id.txtClose)?.setOnClickListener { viewModel.delegate.onCloseClick() }

        viewModel.transactionLiveData.observe(this, Observer { txRecord ->
            if (txRecord != null) {

                val titleText = getString(if (txRecord.incoming) R.string.tx_info_bottom_sheet_title_received else R.string.tx_info_bottom_sheet_title_sent)

                rootView.findViewById<TextView>(R.id.txtTitle)?.text = titleText  + " " + txRecord.amount.coin.code
                context?.let {
                    val coinDrawable = ContextCompat.getDrawable(it, LayoutHelper.getCoinDrawable(txRecord.amount.coin.code))
                    rootView.findViewById<ImageView>(R.id.coinImg)?.setImageDrawable(coinDrawable)
                }

                rootView.findViewById<TextView>(R.id.txtAmount)?.apply {
                    val sign = if (txRecord.incoming) "+" else "-"
                    text = "$sign ${NumberFormatHelper.cryptoAmountFormat.format(Math.abs(txRecord.amount.value))}"
                    setTextColor(resources.getColor(if (txRecord.incoming) R.color.green_crypto else R.color.yellow_crypto, null))
                }

                rootView.findViewById<TextView>(R.id.txDate)?.text = if (txRecord.status == TransactionRecordViewItem.Status.SUCCESS) {
                    DateHelper.getFullDateWithTime(txRecord.date)
                } else {
                    getString(R.string.tx_info_bottom_sheet_status_processing)
                }

                val getStatusText = when {
                    txRecord.status == TransactionRecordViewItem.Status.PENDING -> R.string.tx_info_bottom_sheet_status_processing
                    txRecord.incoming -> R.string.tx_info_bottom_sheet_title_received
                    else -> R.string.tx_info_bottom_sheet_title_sent
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemStatus)?.apply {
                    value = getString(getStatusText)
                    valueIcon = if (txRecord.status == TransactionRecordViewItem.Status.SUCCESS) R.drawable.checkmark else R.drawable.pending
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemTransactionId)?.apply{
                    value = TextHelper.randomHashGenerator()//todo txRecord.hash
                    showValueBackground = true
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.valueWhenReceived)?.apply{
                    value = "\$${NumberFormatHelper.fiatAmountFormat.format(txRecord.valueInBaseCurrency)}"
                }

                rootView.findViewById<TransactionInfoItemView>(R.id.itemFrom)?.apply{
                    value = TextHelper.randomHashGenerator()//todo txRecord.from
                    showValueBackground = true
                }

            }

        })

        viewModel.showDetailsLiveEvent.observe(this, Observer {
            //todo open Details activity
        })

        viewModel.closeLiveEvent.observe(this, Observer {
            dismiss()
        })

        return mDialog as Dialog
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
