package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.transactions.TransactionRecordViewItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
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
        supportActionBar?.title = getString(R.string.FullInfo_Title)

        shareBtn.setOnClickListener { viewModel.delegate.onShareClick() }
        closeBtn.setOnClickListener { onBackPressed() }

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied)
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
//        val txStatus = trx.status
//        val statusTxt = when (txStatus) {
//            is TransactionStatus.Processing -> getString(R.string.Transaction_Info_Processing, txStatus.progress)
//            is TransactionStatus.Pending -> getString(R.string.TransactionInfo_Pending)
//            else -> getString(R.string.TransactionInfo_Completed)
//        }
//        val statusImg = when (txStatus) {
//            is TransactionStatus.Processing -> R.drawable.processing_image
//            is TransactionStatus.Pending -> R.drawable.pending_image
//            else -> R.drawable.comleted_image
//        }
//
//        transactionId.text = "#${trx.hash}"
//        statusImage.setImageResource(statusImg)
//        statusText.text = statusTxt
//
//        itemTime.bind(title = getString(R.string.FullInfo_Time), valueTitle = trx.date?.let { DateHelper.getFullDateWithShortMonth(it) })
//        itemFrom.bind(title = getString(R.string.FullTransactionInfo_From), valueTitle = trx.from, valueIcon = R.drawable.round_person_18px)
//        itemTo.bind(title = getString(R.string.FullTransactionInfo_To), valueTitle = trx.to, valueIcon = R.drawable.round_person_18px)
//        itemAmount.bind(
//                title = getString(R.string.FullTransactionInfo_Amount),
//                valueTitle = ValueFormatter.format(trx.amount, true),
//                valueSubtitle = trx.currencyAmount?.let { ValueFormatter.format(it, true) }
//        )
//        itemFee.bind(title = getString(R.string.FullTransactionInfo_Fee), valueTitle = ValueFormatter.format(trx.fee))
//        itemBlock.bind(title = getString(R.string.FullTransactionInfo_Block), valueTitle = trx.blockHeight.toString())
//
//        transactionId.setOnClickListener { viewModel.delegate.onTransactionIdClick() }
//        itemFrom.setOnClickListener { viewModel.delegate.onFromFieldClick() }
//        itemTo.setOnClickListener { viewModel.delegate.onToFieldClick() }
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
