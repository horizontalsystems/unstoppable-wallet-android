package cash.p.terminal.modules.walletconnect.request.sendtransaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.confirm.ConfirmTransactionScreen
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationFragment
import cash.p.terminal.modules.send.evm.confirmation.SendEvmConfirmationViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsFragment
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewNew
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WCSendEthRequestScreen(
    navController: NavController,
    logger: AppLogger,
    parentNavGraphId: Int,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    peerName: String,
) {
    val transactionRequestViewModel = viewModel<WCSendEthereumTransactionRequestViewModel>()

    val viewModel = viewModel<SendEvmConfirmationViewModel>(
        factory = SendEvmConfirmationViewModel.Factory(
            transactionData = TransactionData(
                transaction.to,
                transaction.value,
                transaction.data
            ),
            additionalInfo = SendEvmData.AdditionalInfo.WalletConnectRequest(
                SendEvmData.WalletConnectInfo(
                    dAppName = peerName,
                    chain = null // todo: need to implement it
                )
            ),
            blockchainType
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = {
            navController.slideFromBottom(
                R.id.sendEvmSettingsFragment,
                SendEvmSettingsFragment.Input(parentNavGraphId)
            )
        },
        onClickClose = null,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            val view = LocalView.current

            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Confirm),
                enabled = uiState.sendEnabled && buttonEnabled,
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)

                        val result = try {
                            logger.info("click send button")
                            val sendResult = viewModel.send()
                            transactionRequestViewModel.approve(sendResult.fullTransaction.transaction.hash)
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            SendEvmConfirmationFragment.Result(true)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                            SendEvmConfirmationFragment.Result(false)
                        }

                        buttonEnabled = true
                        navController.setNavigationResultX(result)
                        navController.popBackStack()
                    }
                }
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = {
                    transactionRequestViewModel.reject()
                    navController.popBackStack()
                }
            )
        }
    ) {
        SendEvmTransactionViewNew(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
        )
    }
}
