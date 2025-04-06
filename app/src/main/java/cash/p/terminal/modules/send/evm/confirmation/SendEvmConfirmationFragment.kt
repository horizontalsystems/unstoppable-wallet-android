package cash.p.terminal.modules.send.evm.confirmation

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import cash.p.terminal.ui_compose.BaseComposeFragment
import io.horizontalsystems.core.requireInput
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.modules.confirm.ConfirmTransactionScreen
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SendEvmConfirmationFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = try {
            navController.requireInput<Input>()
        } catch (e: NullPointerException) {
            navController.popBackStack()
            return
        }
        SendEvmConfirmationScreen(navController, input)
    }

    @Parcelize
    data class Input(
        val transactionDataParcelable: SendEvmModule.TransactionDataParcelable,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val blockchainType: BlockchainType
    ) : Parcelable {
        val transactionData: TransactionData
            get() = TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )

        constructor(
            sendData: SendEvmData,
            blockchainType: BlockchainType
        ) : this(
            SendEvmModule.TransactionDataParcelable(sendData.transactionData),
            sendData.additionalInfo,
            blockchainType
        )
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
private fun SendEvmConfirmationScreen(
    navController: NavController,
    input: SendEvmConfirmationFragment.Input
) {
    val logger = remember { AppLogger("send-evm") }

    val currentBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.sendEvmConfirmationFragment)
    }
    val viewModel = viewModel<SendEvmConfirmationViewModel>(
        viewModelStoreOwner = currentBackStackEntry,
        factory = SendEvmConfirmationViewModel.Factory(
            input.transactionData,
            input.additionalInfo,
            input.blockchainType,
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = { navController.popBackStack() },
        onClickSettings = {
            navController.slideFromBottom(R.id.sendEvmSettingsFragment)
        },
        onClickClose = null,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            val view = LocalView.current

            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
                title = stringResource(R.string.Send_Confirmation_Send_Button),
                onClick = {
                    logger.info("click send button")

                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)

                        val result = try {
                            logger.info("sending tx")
                            viewModel.send()
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
                },
                enabled = uiState.sendEnabled && buttonEnabled
            )
        }
    ) {
        SendEvmTransactionView(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.SendConfirmation
        )
    }
}
