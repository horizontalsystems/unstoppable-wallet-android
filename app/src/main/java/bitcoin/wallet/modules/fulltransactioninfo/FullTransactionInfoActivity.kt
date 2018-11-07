package bitcoin.wallet.modules.fulltransactioninfo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.entities.TransactionStatus
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import bitcoin.wallet.viewHelpers.DateHelper
import bitcoin.wallet.viewHelpers.HudHelper
import bitcoin.wallet.viewHelpers.ValueFormatter
import kotlinx.android.synthetic.main.activity_full_transaction_info.*

class FullTransactionInfoActivity : BaseActivity() {

    private lateinit var viewModel: FullTransactionInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapterId = intent.extras.getString(adapterIdKey)
        val transactionId = intent.extras.getString(transactionIdKey)

        viewModel = ViewModelProviders.of(this).get(FullTransactionInfoViewModel::class.java)
        viewModel.init(adapterId, transactionId)

        setContentView(R.layout.activity_full_transaction_info)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.full_transaction_info_title)

        shareBtn.setOnClickListener { viewModel.delegate.onShareClick() }
        closeBtn.setOnClickListener { onBackPressed() }

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.hud_text_copied, this)
        })

        viewModel.showTransactionRecordViewLiveData.observe(this, Observer { transaction ->
            transaction?.let { setTransaction(it) }
        })

        viewModel.showBlockInfoLiveData.observe(this, Observer {
            //todo show BlockInfo
        })

        viewModel.shareTransactionLiveData.observe(this, Observer {
            Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show()
            //todo open share dialog
            //shared link is not available yet(this feature is in progress)
        })
    }

    private fun setTransaction(trx: TransactionRecordViewItem) {
        val txStatus = trx.status
        val statusTxt = when (txStatus) {
            is TransactionStatus.Processing -> getString(R.string.transaction_info_processing, txStatus.progress)
            is TransactionStatus.Pending -> getString(R.string.transaction_info_status_pending)
            else -> getString(R.string.transaction_info_status_completed)
        }
        val statusImg = when (txStatus) {
            is TransactionStatus.Processing -> R.drawable.processing_image
            is TransactionStatus.Pending -> R.drawable.pending_image
            else -> R.drawable.comleted_image
        }

        transactionId.text = "#${trx.hash}"
        statusImage.setImageResource(statusImg)
        statusText.text = statusTxt

        itemTime.bind(title = getString(R.string.full_transaction_info_time), valueTitle = trx.date?.let { DateHelper.getFullDateWithShortMonth(it) })
        itemFrom.bind(title = getString(R.string.full_transaction_info_from), valueTitle = trx.from, valueIcon = R.drawable.round_person_18px)
        itemTo.bind(title = getString(R.string.full_transaction_info_to), valueTitle = trx.to, valueIcon = R.drawable.round_person_18px)
        itemAmount.bind(
                title = getString(R.string.full_transaction_info_amount),
                valueTitle = ValueFormatter.format(trx.amount, true),
                valueSubtitle = trx.currencyAmount?.let { ValueFormatter.format(it, true) }
        )
        itemFee.bind(title = getString(R.string.full_transaction_info_fee), valueTitle = ValueFormatter.format(trx.fee))
        itemBlock.bind(title = getString(R.string.full_transaction_info_block), valueTitle = trx.blockHeight.toString())

        transactionId.setOnClickListener { viewModel.delegate.onTransactionIdClick() }
        itemFrom.setOnClickListener { viewModel.delegate.onFromFieldClick() }
        itemTo.setOnClickListener { viewModel.delegate.onToFieldClick() }
    }

    companion object {
        const val adapterIdKey = "adapter_id"
        const val transactionIdKey = "transaction_id"

        fun start(context: Context, adapterId: String, transactionId: String) {
            val intent = Intent(context, FullTransactionInfoActivity::class.java)
            intent.putExtra(adapterIdKey, adapterId)
            intent.putExtra(transactionIdKey, transactionId)
            context.startActivity(intent)
        }
    }
}
