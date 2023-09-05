package cash.p.terminal.modules.send.evm.confirmation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsFragment
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData

class SendEvmConfirmationFragment : BaseComposeFragment() {

    private val logger = AppLogger("send-evm")

    private val vmFactory by lazy {
        val evmKitWrapperViewModel by navGraphViewModels<EvmKitWrapperHoldingViewModel>(sendNavGraphId)
        SendEvmConfirmationModule.Factory(
            evmKitWrapperViewModel.evmKitWrapper,
            SendEvmData(transactionData, additionalInfo)
        )
    }
    private val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(R.id.sendEvmConfirmationFragment) { vmFactory }
    private val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.sendEvmConfirmationFragment) { vmFactory }
    private val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(R.id.sendEvmConfirmationFragment) { vmFactory }

    private var snackbarInProcess: CustomSnackbar? = null

    private val sendNavGraphId: Int by lazy { arguments?.getInt(SendEvmModule.sendNavGraphIdKey)!! }
    private val sendEntryPointDestId: Int by lazy { arguments?.getInt(SendEvmModule.sendEntryPointDestIdKey) ?: 0 }
    private val closeUntilDestId: Int by lazy {
        if (sendEntryPointDestId == 0) {
            sendNavGraphId
        } else {
            sendEntryPointDestId
        }
    }


    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable = arguments?.parcelable<SendEvmModule.TransactionDataParcelable>(SendEvmModule.transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
        }
    private val additionalInfo: SendEvmData.AdditionalInfo?
        get() = arguments?.parcelable(SendEvmModule.additionalInfoKey)

    @Composable
    override fun GetContent() {
        SendEvmConfirmationScreen(
            sendEvmTransactionViewModel = sendEvmTransactionViewModel,
            feeViewModel = feeViewModel,
            nonceViewModel = nonceViewModel,
            parentNavGraphId = R.id.sendEvmConfirmationFragment,
            navController = findNavController(),
            onSendClick = {
                logger.info("click send button")
                sendEvmTransactionViewModel.send(logger)
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendEvmTransactionViewModel.sendingLiveData.observe(viewLifecycleOwner) {
            snackbarInProcess = HudHelper.showInProcessMessage(
                requireView(),
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) {
            HudHelper.showSuccessMessage(
                requireActivity().findViewById(android.R.id.content),
                R.string.Hud_Text_Done
            )
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack(closeUntilDestId, true)
            }, 1200)
        }

        sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)

            findNavController().popBackStack()
        }
    }

}

@Composable
private fun SendEvmConfirmationScreen(
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    nonceViewModel: SendEvmNonceViewModel,
    parentNavGraphId: Int,
    navController: NavController,
    onSendClick: () -> Unit
) {
    val enabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                            icon = R.drawable.ic_manage_2,
                            tint = ComposeAppTheme.colors.jacob,
                            onClick = {
                                navController.slideFromBottom(
                                    resId = R.id.sendEvmSettingsFragment,
                                    args = SendEvmSettingsFragment.prepareParams(parentNavGraphId)
                                )
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
                        nonceViewModel,
                        navController
                    )
                }
                ButtonsGroupWithShade {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        title = stringResource(R.string.Send_Confirmation_Send_Button),
                        onClick = onSendClick,
                        enabled = enabled
                    )
                }
            }
        }
    }
}
