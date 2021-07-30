package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoAdapter
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsPresenter
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_transaction_info.*
import java.util.*

class TransactionInfoFragment : BaseFragment(), TransactionInfoAdapter.Listener {

    private val viewModelTxs by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)
    private val viewModel by viewModels<TransactionInfoViewModel> { TransactionInfoModule.Factory((viewModelTxs.delegate as TransactionsPresenter).itemDetails!!) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transaction_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val itemsAdapter =
            TransactionInfoAdapter(viewModel.viewItemsLiveData, viewLifecycleOwner, this)
        recyclerView.adapter = ConcatAdapter(itemsAdapter)

        btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.showTransactionLiveEvent.observe(this, Observer { url ->
            openUrlInCustomTabs(url)
        })

        viewModel.showShareLiveEvent.observe(viewLifecycleOwner, { value ->
            context?.startActivity(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, value)
                type = "text/plain"
            })
        })

        viewModel.copyRawTransactionLiveEvent.observe(viewLifecycleOwner, { rawTransaction ->
            copyText(rawTransaction)
        })
    }

    override fun onAddressClick(address: String) {
        copyText(address)
    }

    override fun onActionButtonClick(actionButton: TransactionInfoActionButton) {
        viewModel.onActionButtonClick(actionButton)
    }

    override fun onAdditionalButtonClick(buttonType: TransactionInfoButtonType) {
        viewModel.onAdditionalButtonClick(buttonType)
    }

    override fun onLockInfoClick(lockDate: Date) {
        context?.let {
            val title = it.getString(R.string.Info_LockTime_Title)
            val description = it.getString(
                R.string.Info_LockTime_Description,
                DateHelper.getFullDate(lockDate)
            )
            val infoParameters = InfoParameters(title, description)

            findNavController().navigate(R.id.infoFragment, InfoFragment.arguments(infoParameters))
        }
    }

    override fun onDoubleSpendInfoClick(transactionHash: String, conflictingHash: String) {
        context?.let {
            val title = it.getString(R.string.Info_DoubleSpend_Title)
            val description = it.getString(R.string.Info_DoubleSpend_Description)
            val infoParameters = InfoParameters(title, description, transactionHash, conflictingHash)

            findNavController().navigate(
                R.id.infoFragment,
                InfoFragment.arguments(infoParameters)
            )
        }
    }

    override fun closeClick() {
        findNavController().popBackStack()
    }

    override fun onClickStatusInfo() {
        findNavController().navigate(R.id.statusInfoDialog)
    }

    private fun copyText(address: String) {
        TextHelper.copyText(address)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
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
            builder.setExitAnimations(
                ctx,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )

            val customTabsIntent = builder.build()

            customTabsIntent.launchUrl(ctx, Uri.parse(url))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
    }
}
