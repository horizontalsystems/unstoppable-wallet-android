package cash.p.terminal.modules.swap.approve.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.SendEvmModule.additionalInfoKey
import cash.p.terminal.modules.send.evm.SendEvmModule.backButtonKey
import cash.p.terminal.modules.send.evm.SendEvmModule.blockchainTypeKey
import cash.p.terminal.modules.send.evm.SendEvmModule.transactionDataKey
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.approve.SwapApproveModule
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.HsIconButton
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.setNavigationResult
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType

class SwapApproveConfirmationFragment : BaseFragment() {
    private val logger = AppLogger("swap-approve")
    private val additionalItems: SendEvmData.AdditionalInfo?
        get() = arguments?.getParcelable(additionalInfoKey)

    private val blockchainType: BlockchainType?
        get() = arguments?.getParcelable(blockchainTypeKey)

    private val backButton: Boolean
        get() = arguments?.getBoolean(backButtonKey) ?: true

    private val vmFactory by lazy {
        SwapApproveConfirmationModule.Factory(
            SendEvmData(transactionData, additionalItems),
            blockchainType!!
        )
    }
    private val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.swapApproveConfirmationFragment) { vmFactory }
    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable =
                arguments?.getParcelable<SendEvmModule.TransactionDataParcelable>(transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
        }

    private var snackbarInProcess: CustomSnackbar? = null

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
                SwapApproveConfirmationScreen(
                    sendEvmTransactionViewModel = sendEvmTransactionViewModel,
                    feeViewModel = feeViewModel,
                    parentNavGraphId = R.id.swapApproveConfirmationFragment,
                    navController = findNavController(),
                    onSendClick = {
                        logger.info("click approve button")
                        sendEvmTransactionViewModel.send(logger)
                    },
                    backButton = backButton
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Swap_Approving,
                SnackbarDuration.INDEFINITE
            )
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().setNavigationResult(
                    SwapApproveModule.requestKey,
                    bundleOf(SwapApproveModule.resultKey to true),
                    R.id.swapFragment
                )
                findNavController().popBackStack(R.id.swapFragment, false)
            }, 1200)
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        }

    }
}

@Composable
private fun SwapApproveConfirmationScreen(
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    parentNavGraphId: Int,
    navController: NavController,
    onSendClick: () -> Unit,
    backButton: Boolean
) {
    val enabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                val navigationIcon: @Composable (() -> Unit)? = if (backButton) {
                    {
                        HsIconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "back button",
                                tint = ComposeAppTheme.colors.jacob
                            )
                        }
                    }
                } else {
                    null
                }

                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                    navigationIcon = navigationIcon,
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = {
                                navController.popBackStack(R.id.swapFragment, false)
                            }
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    SendEvmTransactionView(
                        sendEvmTransactionViewModel,
                        feeViewModel,
                        navController,
                        parentNavGraphId,
                    )
                }
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        title = stringResource(R.string.Swap_Approve),
                        onClick = onSendClick,
                        enabled = enabled
                    )
                }
            }
        }
    }
}
