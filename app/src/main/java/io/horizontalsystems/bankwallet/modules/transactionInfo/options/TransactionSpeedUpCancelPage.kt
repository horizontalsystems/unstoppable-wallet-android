package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorSheet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionView
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.rememberAsyncAction
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class TransactionSpeedUpCancelPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        TransactionSpeedUpCancelScreen(navController, input)
    }

    @Serializable
    data class Input(
        val blockchainType: BlockchainType,
        val optionType: SpeedUpCancelType,
        val transactionHash: String
    )

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}

@Composable
private fun TransactionSpeedUpCancelScreen(
    navController: HSNavigation,
    input: TransactionSpeedUpCancelPage.Input
) {
    val resultEventBus = LocalResultEventBus.current
    val logger = remember { AppLogger("tx-speedUp-cancel") }
    val view = LocalView.current

    val viewModel = viewModel<TransactionSpeedUpCancelViewModel>(
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
            navController.removeLastOrNull()
        }
    }

    val sendTransactionState = uiState.sendTransactionState

    ConfirmTransactionScreen(
        title = viewModel.title,
        initialLoading = uiState.initialLoading,
        onClickBack = { navController.removeLastOrNull() },
        onClickFeeSettings = {
            navController.slideFromBottom(TransactionSpeedUpCancelTransactionSettingsPage)
        },
        buttonsSlot = {
            val sendAction = rememberAsyncAction()

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = if (sendAction.inProgress) stringResource(R.string.Send_Sending) else viewModel.buttonTitle,
                onClick = {
                    logger.info("click ${viewModel.buttonTitle} button")

                    sendAction.run {
                        try {
                            logger.info("sending tx")
                            viewModel.send()
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            resultEventBus.sendResult(TransactionSpeedUpCancelPage.Result(true))
                            navController.removeLastOrNull()
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            navController.slideFromBottom(ErrorSheet(
                                ErrorSheet.Input(t.message ?: t.javaClass.simpleName)
                            ))
                        }
                    }
                },
                enabled = !sendAction.inProgress && uiState.sendEnabled
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
