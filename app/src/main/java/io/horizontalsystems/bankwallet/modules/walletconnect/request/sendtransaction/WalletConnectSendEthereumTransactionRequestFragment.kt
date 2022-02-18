package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.AmountCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.SubheadCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleHexValueCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleTypedValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class WalletConnectSendEthereumTransactionRequestFragment : BaseFragment() {
    private val logger = AppLogger("wallet-connect")
    private val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)
    val vmFactory by lazy {
        WalletConnectRequestModule.Factory(
            baseViewModel.sharedSendEthereumTransactionRequest!!, baseViewModel.service
        )
    }
    private val viewModel by viewModels<WalletConnectSendEthereumTransactionRequestViewModel> { vmFactory }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.walletConnectSendEthereumTransactionRequestFragment) { vmFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                RequestScreen(
                    findNavController(),
                    baseViewModel,
                    viewModel,
                    sendEvmTransactionViewModel,
                    feeViewModel,
                    logger,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
            close()
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
            viewModel.approve(transactionHash)
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            close()
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

        //todo Implement Merged changes
//        binding.sendEvmTransactionView.init(
//            sendEvmTransactionViewModel,
//            feeViewModel,
//            viewLifecycleOwner,
//            findNavController(),
//            R.id.walletConnectSendEthereumTransactionRequestFragment
//        )
    }

    private fun close() {
        baseViewModel.sharedSendEthereumTransactionRequest = null
        findNavController().popBackStack()
    }

}

@Composable
private fun RequestScreen(
    navController: NavController,
    baseViewModel: WalletConnectViewModel,
    viewModel: WalletConnectSendEthereumTransactionRequestViewModel,
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    logger: AppLogger,
) {

    val title by sendEvmTransactionViewModel.transactionTitleLiveData.observeAsState("")
    val transactionInfoItems by sendEvmTransactionViewModel.viewItemsLiveData.observeAsState()
    val fee by feeViewModel.feeLiveData.observeAsState("")
    val approveEnabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.PlainString(title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(12.dp))
                transactionInfoItems?.let { sections ->
                    sections.forEach { section ->
                        CellSingleLineLawrenceSection(section.viewItems) { item ->
                            when (item) {
                                is ViewItem.Subhead -> SubheadCell(item.title, item.value)
                                is ViewItem.Value -> TitleTypedValueCell(
                                    item.title,
                                    item.value,
                                    item.type
                                )
                                is ViewItem.Address -> TitleHexValueCell(
                                    item.title,
                                    item.valueTitle,
                                    item.value
                                )
                                is ViewItem.Input -> TitleHexValueCell(
                                    Translator.getString(R.string.WalletConnect_Input),
                                    item.value,
                                    item.value
                                )
                                is ViewItem.Amount -> AmountCell(
                                    item.fiatAmount,
                                    item.coinAmount,
                                    item.type
                                )
                                is ViewItem.Warning -> TextImportantWarning(
                                    text = item.description,
                                    title = item.title,
                                    icon = item.icon
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                //todo Implement merged changes
//                EvmFeeCell(
//                    title = stringResource(R.string.FeeSettings_MaxFee),
//                    value = fee,
//                    loading = false,
//                ) {
//                    navController.slideFromBottom(
//                        resId = R.id.sendEvmFeeSettingsFragment,
//                        args = SendEvmFeeSettingsFragment.prepareParams(R.id.walletConnectSendEthereumTransactionRequestFragment)
//                    )
//                }

                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Confirm),
                    enabled = approveEnabled,
                    onClick = {
                        logger.info("click confirm button")
                        sendEvmTransactionViewModel.send(logger)
                    }
                )
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Reject),
                    onClick = {
                        viewModel.reject()
                        baseViewModel.sharedSendEthereumTransactionRequest = null
                        navController.popBackStack()
                    }
                )
                Spacer(Modifier.height(32.dp))
            }

        }
    }
}
