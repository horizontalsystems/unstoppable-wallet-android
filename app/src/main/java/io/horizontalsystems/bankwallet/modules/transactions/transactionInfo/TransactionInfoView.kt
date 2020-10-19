package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.views.FullTransactionInfoFragment
import io.horizontalsystems.bankwallet.modules.info.InfoFragment
import io.horizontalsystems.bankwallet.modules.info.InfoParameters
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewHelper
import io.horizontalsystems.bankwallet.ui.extensions.ConstraintLayoutWithHeader
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarGravity
import kotlinx.android.synthetic.main.transaction_info_bottom_sheet.view.*

class TransactionInfoView : ConstraintLayoutWithHeader {

    private lateinit var viewModel: TransactionInfoViewModel
    private lateinit var lifecycleOwner: LifecycleOwner
    private var listener: Listener? = null

    interface Listener {
        fun openTransactionInfo()
        fun closeTransactionInfo()
        fun onShowInfoMessage(snackbar: CustomSnackbar? = null)
        fun showFragmentInTopContainerView(fragment: Fragment)
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

        txtFullInfo.setOnSingleClickListener {
            viewModel.delegate.openFullInfo()
        }

        val transactionDetailsAdapter = TransactionDetailsAdapter(viewModel)
        rvDetails.adapter = transactionDetailsAdapter

        viewModel.showCopiedLiveEvent.observe(lifecycleOwner, Observer {
            val snackbar = HudHelper.showSuccessMessage(this, R.string.Hud_Text_Copied, gravity = SnackbarGravity.TOP_OF_VIEW)
            listener?.onShowInfoMessage(snackbar)
        })

        viewModel.showShareLiveEvent.observe(lifecycleOwner, Observer { url ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            }
            context.startActivity(sendIntent)
        })


        viewModel.showFullInfoLiveEvent.observe(lifecycleOwner, Observer { (txHash, wallet) ->
            listener?.showFragmentInTopContainerView(FullTransactionInfoFragment.instance(txHash, wallet))
        })

        viewModel.showLockInfo.observe(lifecycleOwner, Observer { lockDate ->
            val title = context.getString(R.string.Info_LockTime_Title)
            val description = context.getString(R.string.Info_LockTime_Description, DateHelper.getFullDate(lockDate))
            val infoParameters = InfoParameters(title, description)

            listener?.showFragmentInTopContainerView(InfoFragment.instance(infoParameters))
        })

        viewModel.showDoubleSpendInfo.observe(lifecycleOwner, Observer { (txHash, conflictingTxHash) ->
            val title = context.getString(R.string.Info_DoubleSpend_Title)
            val description = context.getString(R.string.Info_DoubleSpend_Description)
            val infoParameters = InfoParameters(title, description, txHash, conflictingTxHash)

            listener?.showFragmentInTopContainerView(InfoFragment.instance(infoParameters))
        })

        viewModel.titleLiveData.observe(lifecycleOwner, Observer { titleViewItem ->
            val title = if (titleViewItem.type == TransactionType.Approve) R.string.TransactionInfo_Approval else R.string.TransactionInfo_Transaction
            setTitle(context.getString(title))
            setSubtitle(titleViewItem.date?.let { DateHelper.getFullDate(it) })
            setHeaderIcon(TransactionViewHelper.getTransactionTypeIcon(titleViewItem.type))

            sentToSelfIcon.isVisible = titleViewItem.type == TransactionType.SentToSelf

            primaryValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, TransactionViewHelper.getLockIcon(titleViewItem.lockState), 0)
            primaryValue.setTextColor(TransactionViewHelper.getAmountColor(titleViewItem.type, context))

            titleViewItem.primaryAmountInfo.let {
                primaryName.text = it.getAmountName()
                primaryValue.text = it.getFormattedForTxInfo()
            }

            titleViewItem.secondaryAmountInfo.let {
                secondaryName.text = it?.getAmountName()
                secondaryValue.text = it?.getFormattedForTxInfo()
            }
        })

        viewModel.detailsLiveData.observe(lifecycleOwner, Observer {
            transactionDetailsAdapter.setItems(it)
            listener?.openTransactionInfo()
        })
    }
}
