package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.authorizedAction
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.xtransaction.cells.HeaderCell
import io.horizontalsystems.bankwallet.modules.xtransaction.helpers.TransactionInfoHelper
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.BurnSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.FeeSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.MintSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.ReceiveCoinSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.SendCoinSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.SwapSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.ton.ContractCallSection
import io.horizontalsystems.bankwallet.modules.xtransaction.sections.ton.ContractDeploySection
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TonConnectSendRequestScreen(navController: NavController) {
    val logger = remember { AppLogger("ton-connect request") }
    val mainActivityViewModel =
        viewModel<MainActivityViewModel>(viewModelStoreOwner = LocalContext.current as ComponentActivity)
    val viewModel = viewModel<TonConnectSendRequestViewModel>(initializer = {
        val sendRequestEntity = mainActivityViewModel.tcSendRequest.value
        mainActivityViewModel.onTcSendRequestHandled()

        TonConnectSendRequestViewModel(
            sendRequestEntity,
            App.accountManager,
            App.tonConnectManager
        )
    })

    val uiState = viewModel.uiState

    ConfirmTransactionScreen(
        onClickBack = navController::popBackStack,
        onClickSettings = null,
        onClickClose = null,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            val view = LocalView.current

            if (uiState.error != null) {
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    enabled = true,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            } else {
                var buttonEnabled by remember { mutableStateOf(true) }

                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Confirm),
                    enabled = uiState.confirmEnabled && buttonEnabled,
                    onClick = {
                        navController.authorizedAction {
                            coroutineScope.launch {
                                buttonEnabled = false
                                HudHelper.showInProcessMessage(
                                    view,
                                    R.string.Send_Sending,
                                    SnackbarDuration.INDEFINITE
                                )

                                try {
                                    logger.info("click confirm button")
                                    viewModel.confirm()
                                    logger.info("success")

                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                                    delay(1200)
                                } catch (t: Throwable) {
                                    logger.warning("failed", t)
                                    HudHelper.showErrorMessage(view, t.message ?: t.javaClass.simpleName)
                                }

                                buttonEnabled = true
                                navController.popBackStack()
                            }
                        }
                    }
                )
                VSpacer(16.dp)
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Reject),
                    enabled = uiState.rejectEnabled,
                    onClick = {
                        viewModel.reject()
                        navController.popBackStack()
                    }
                )
            }
        }
    ) {
        uiState.error?.let { error ->
            TextImportantError(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = error.message ?: error.javaClass.simpleName
            )
        }

        Crossfade(uiState.tonTransactionRecord) { record ->
            if (record != null) {
                val transactionInfoHelper = remember {
                    TransactionInfoHelper()
                }

                Column {
                    record.actions.forEachIndexed { index, action ->
                        if (index != 0) {
                            VSpacer(12.dp)
                        }
                        TonConnectRequestActionSection(
                            action = action,
                            transactionInfoHelper = transactionInfoHelper,
                            navController = navController
                        )
                    }
                    VSpacer(12.dp)

                    FeeSection(
                        transactionInfoHelper = transactionInfoHelper,
                        fee = record.fee,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun TonConnectRequestActionSection(
    action: TonTransactionRecord.Action,
    transactionInfoHelper: TransactionInfoHelper,
    navController: NavController,
) {
    when (val actionType = action.type) {
        is TonTransactionRecord.Action.Type.Burn -> {
            BurnSection(
                transactionValue = actionType.value,
                transactionInfoHelper = transactionInfoHelper,
                navController = navController
            )
        }

        is TonTransactionRecord.Action.Type.ContractCall -> {
            ContractCallSection(
                navController = navController,
                operation = actionType.operation,
                address = actionType.address,
                transactionValue = actionType.value,
                transactionInfoHelper = transactionInfoHelper,
                blockchainType = BlockchainType.Ton
            )
        }

        is TonTransactionRecord.Action.Type.ContractDeploy -> {
            ContractDeploySection(
                interfaces = actionType.interfaces
            )
        }

        is TonTransactionRecord.Action.Type.Mint -> {
            MintSection(
                transactionValue = actionType.value,
                transactionInfoHelper = transactionInfoHelper,
                navController = navController
            )
        }

        is TonTransactionRecord.Action.Type.Receive -> {
            ReceiveCoinSection(
                transactionValue = actionType.value,
                address = actionType.from,
                comment = actionType.comment,
                statPage = StatPage.TonConnect,
                navController = navController,
                transactionInfoHelper = transactionInfoHelper,
                blockchainType = BlockchainType.Ton
            )
        }

        is TonTransactionRecord.Action.Type.Send -> {
            SendCoinSection(
                transactionValue = actionType.value,
                address = actionType.to,
                comment = actionType.comment,
                sentToSelf = actionType.sentToSelf,
                statPage = StatPage.TonConnect,
                navController = navController,
                transactionInfoHelper = transactionInfoHelper,
                blockchainType = BlockchainType.Ton
            )
        }

        is TonTransactionRecord.Action.Type.Swap -> {
            SwapSection(
                transactionInfoHelper = transactionInfoHelper,
                navController = navController,
                transactionValueIn = actionType.valueIn,
                transactionValueOut = actionType.valueOut
            )
        }

        is TonTransactionRecord.Action.Type.Unsupported -> {
            SectionUniversalLawrence {
                HeaderCell(
                    title = stringResource(R.string.Send_Confirmation_Action),
                    value = actionType.type,
                    painter = null
                )
            }
        }
    }
}
