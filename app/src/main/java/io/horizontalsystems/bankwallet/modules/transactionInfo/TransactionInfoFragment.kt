package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoAdapter
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionSpeedUpCancelFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_transaction_info.*
import java.util.*

class TransactionInfoFragment : BaseFragment(), TransactionInfoAdapter.Listener {

    private val viewModelTxs by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment)
    private val viewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment) {
        TransactionInfoModule.Factory(viewModelTxs.tmpItemToShow)
    }

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

        viewModel.openTransactionOptionsModule.observe(viewLifecycleOwner, { (optionType, txHash) ->
            val params = TransactionSpeedUpCancelFragment.prepareParams(optionType, txHash)
            findNavController().navigate(R.id.transactionInfoFragment_to_transactionSpeedUpCancelFragment, params, navOptions())
        })

        buttonCloseCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        buttonCloseCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    title = getString(R.string.Button_Close),
                    onClick = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    override fun onAddressClick(address: String) {
        copyText(address)
    }

    override fun onActionButtonClick(actionButton: TransactionInfoActionButton) {
        viewModel.onActionButtonClick(actionButton)
    }

    override fun onUrlClick(url: String) {
        context?.let { ctx ->
            LinkHelper.openLinkInAppBrowser(ctx, url)
        }
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

    override fun onOptionButtonClick(optionType: TransactionInfoOption.Type) {
        viewModel.onOptionButtonClick(optionType)
    }

    private fun copyText(address: String) {
        TextHelper.copyText(address)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
    }
}
