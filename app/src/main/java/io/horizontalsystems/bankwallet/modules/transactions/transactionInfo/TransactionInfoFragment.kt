package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewHelper
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsPresenter
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.core.dismissOnBackPressed
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setOnSingleClickListener
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarGravity
import kotlinx.android.synthetic.main.fragment_transaction_info.*

class TransactionInfoFragment : BottomSheetDialogFragment() {

    private val viewModelTxs by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)
    private val viewModel by viewModels<TransactionInfoViewModel>()
    private var snackBar: CustomSnackbar? = null

    override fun getTheme() = R.style.BottomDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.dismissOnBackPressed { dismiss() }
        dialog?.setOnShowListener {
            dialog?.findViewById<View>(R.id.design_bottom_sheet)?.let { view ->
                BottomSheetBehavior.from(view).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    isHideable = true
                    skipCollapsed = true
                }
            }
        }

        return inflater.inflate(R.layout.fragment_transaction_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (viewModelTxs.delegate as TransactionsPresenter).itemDetails?.let {
            viewModel.init(it.record, it.wallet)
            setTransactionInfoDialog()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        snackBar?.dismiss()
        findNavController().popBackStack()
    }

    private fun setTransactionInfoDialog() {
        txtViewOnExplorer.setOnSingleClickListener {
            viewModel.delegate.openExplorer()
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        val transactionDetailsAdapter = TransactionDetailsAdapter(viewModel)
        rvDetails.adapter = transactionDetailsAdapter

        viewModel.explorerButton.observe(this, Observer { (explorerName, enabled) ->
            txtViewOnExplorer.text = getString(R.string.TransactionInfo_ButtonViewOnExplorerName, explorerName)
            txtViewOnExplorer.isEnabled = enabled
        })

        viewModel.showCopiedLiveEvent.observe(this, Observer {
            snackBar = HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Copied, gravity = SnackbarGravity.TOP_OF_VIEW)
        })

        viewModel.showShareLiveEvent.observe(this, Observer { url ->
            context?.startActivity(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, url)
                type = "text/plain"
            })
        })

        viewModel.showLockInfo.observe(this, Observer { lockDate ->
            context?.let {
                val title = it.getString(R.string.Info_LockTime_Title)
                val description = it.getString(R.string.Info_LockTime_Description, DateHelper.getFullDate(lockDate))
                val infoParameters = InfoParameters(title, description)

                findNavController().navigate(R.id.infoFragment, InfoFragment.arguments(infoParameters))
            }
        })

        viewModel.showStatusInfoLiveEvent.observe(this, Observer {
            findNavController().navigate(R.id.statusInfoDialog)
        })

        viewModel.showTransactionLiveEvent.observe(this, Observer { url ->
            openUrlInCustomTabs(url)
        })

        viewModel.showDoubleSpendInfo.observe(this, Observer { (txHash, conflictingTxHash) ->
            context?.let {
                val title = it.getString(R.string.Info_DoubleSpend_Title)
                val description = it.getString(R.string.Info_DoubleSpend_Description)
                val infoParameters = InfoParameters(title, description, txHash, conflictingTxHash)

                findNavController().navigate(R.id.infoFragment, InfoFragment.arguments(infoParameters))
            }
        })

        viewModel.titleLiveData.observe(this, Observer { titleViewItem ->
            val title = if (titleViewItem.type == TransactionType.Approve) R.string.TransactionInfo_Approval else R.string.TransactionInfo_Transaction
            txtTitle.text = context?.getString(title)
            txtSubtitle.text = titleViewItem.date?.let { DateHelper.getFullDate(it) }
            headerIcon.setImageResource(TransactionViewHelper.getTransactionTypeIcon(titleViewItem.type))

            sentToSelfIcon.isVisible = titleViewItem.type == TransactionType.SentToSelf

            primaryValue.setCompoundDrawablesWithIntrinsicBounds(0, 0, TransactionViewHelper.getLockIcon(titleViewItem.lockState), 0)
            primaryValue.setTextColor(TransactionViewHelper.getAmountColor(titleViewItem.type, requireContext()))

            titleViewItem.primaryAmountInfo.let {
                primaryName.text = it.getAmountName()
                primaryValue.text = it.getFormattedForTxInfo()
            }

            titleViewItem.secondaryAmountInfo.let {
                secondaryName.text = it?.getAmountName()
                secondaryValue.text = it?.getFormattedForTxInfo()
            }
        })

        viewModel.detailsLiveData.observe(this, Observer {
            transactionDetailsAdapter.setItems(it)
        })
    }

    private fun openUrlInCustomTabs(url: String) {
        context?.let { ctx ->
            val builder = CustomTabsIntent.Builder()

            val color = ctx.getColor(R.color.tyler)

            val params = CustomTabColorSchemeParams.Builder()
                    .setNavigationBarColor(color)
                    .setToolbarColor(color)
                    .build()

            builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params)
            builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, params)
            builder.setStartAnimations(ctx, R.anim.slide_from_right, R.anim.slide_to_left)
            builder.setExitAnimations(ctx, android.R.anim.slide_in_left, android.R.anim.slide_out_right)

            val customTabsIntent = builder.build()

            customTabsIntent.launchUrl(ctx, Uri.parse(url))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rvDetails.adapter = null
    }
}
