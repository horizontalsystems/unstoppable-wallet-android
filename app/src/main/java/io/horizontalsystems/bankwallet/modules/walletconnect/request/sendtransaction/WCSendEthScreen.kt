package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WCSendEthRequestScreen(
    navController: NavController,
    logger: AppLogger,
    blockchainType: BlockchainType,
    transaction: WalletConnectTransaction,
    peerName: String,
) {
    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.wcRequestFragment)
    }
    val viewModel = viewModel<WCSendEthereumTransactionRequestViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = WCSendEthereumTransactionRequestViewModel.Factory(
            blockchainType = blockchainType,
            transaction = transaction,
            peerName = peerName
        )
    )
    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = {
            navController.slideFromBottom(R.id.wcSendEvmTransactionSettings)
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

                        try {
                            logger.info("click confirm button")
                            viewModel.confirm()
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                        }

                        buttonEnabled = true
                        navController.popBackStack()
                    }
                }
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = {
                    viewModel.reject()
                    navController.popBackStack()
                }
            )
        }
    ) {
        SendEvmTransactionView(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
            StatPage.WalletConnect
        )
    }
}
