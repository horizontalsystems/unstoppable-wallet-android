package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class TransactionSpeedUpCancelFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            TransactionSpeedUpCancelScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val optionType: SpeedUpCancelType,
        val transactionHash: String
    ) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
private fun TransactionSpeedUpCancelScreen(
    navController: NavController,
    input: TransactionSpeedUpCancelFragment.Input
) {
    val logger = remember { AppLogger("tx-speedUp-cancel") }
    val view = LocalView.current

    val viewModelStoreOwner = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.transactionSpeedUpCancelFragment)
    }

    val viewModel = viewModel<TransactionSpeedUpCancelViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = TransactionSpeedUpCancelViewModel.Factory(
            input.blockchainType,
            input.transactionHash,
            input.optionType,
        )
    )

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.error) {
        if (uiState.error is TransactionAlreadyInBlock) {
            HudHelper.showErrorMessage(view, R.string.TransactionInfoOptions_Warning_TransactionInBlock)
            navController.popBackStack()
        }
    }

    val sendTransactionState = uiState.sendTransactionState

    ConfirmTransactionScreen(
        title = viewModel.title,
        onClickBack = { navController.popBackStack() },
        onClickSettings = {
            navController.slideFromBottom(R.id.transactionSpeedUpCancelTransactionSettings)
        },
        onClickClose = null,
        buttonsSlot = {
            val buttonTitle = viewModel.buttonTitle
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = buttonTitle,
                onClick = {
                    logger.info("click $buttonTitle button")

                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)

                        val result = try {
                            logger.info("sending tx")
                            viewModel.send()
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            TransactionSpeedUpCancelFragment.Result(true)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                            TransactionSpeedUpCancelFragment.Result(false)
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
            sendTransactionState.cautions,
            sendTransactionState.fields,
            sendTransactionState.networkFee,
            StatPage.Resend
        )
    }
}
