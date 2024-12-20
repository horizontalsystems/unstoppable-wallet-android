package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
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
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.xtransaction.TransactionInfoHelper
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxBurnSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxContractCallSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxFeeSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxMintSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxReceiveCoinSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxSendCoinSection
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxSwapSection
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
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
            App.tonConnectManager,
            App.marketKit,
            App.currencyManager
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

            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Confirm),
                enabled = uiState.confirmEnabled && buttonEnabled,
                onClick = {
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
                enabled = uiState.rejectEnabled,
                onClick = {
                    viewModel.reject()
                    navController.popBackStack()
                }
            )
        }
    ) {
        uiState.error?.let { error ->
            TextImportantError(text = error.message ?: error.javaClass.simpleName)
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
                        when (val actionType = action.type) {
                            is TonTransactionRecord.Action.Type.Burn -> {
                                XxxBurnSection(
                                    transactionValue = actionType.value,
                                    transactionInfoHelper = transactionInfoHelper,
                                    navController = navController
                                )
                            }

                            is TonTransactionRecord.Action.Type.ContractCall -> {
                                XxxContractCallSection(
                                    navController = navController,
                                    operation = actionType.operation,
                                    address = actionType.address,
                                    transactionValue = actionType.value,
                                    transactionInfoHelper = transactionInfoHelper
                                )
                            }

                            is TonTransactionRecord.Action.Type.ContractDeploy -> {
                                Text("TODO")
                            }

                            is TonTransactionRecord.Action.Type.Mint -> {
                                XxxMintSection(
                                    transactionValue = actionType.value,
                                    transactionInfoHelper = transactionInfoHelper,
                                    navController = navController
                                )
                            }

                            is TonTransactionRecord.Action.Type.Receive -> {
                                XxxReceiveCoinSection(
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
                                XxxSendCoinSection(
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
                                XxxSwapSection(
                                    transactionInfoHelper = transactionInfoHelper,
                                    navController = navController,
                                    transactionValueIn = actionType.valueIn,
                                    transactionValueOut = actionType.valueOut
                                )
                            }

                            is TonTransactionRecord.Action.Type.Unsupported -> {
                                Text("TODO")
                            }
                        }

                    }

                    VSpacer(12.dp)

                    XxxFeeSection(
                        transactionInfoHelper = transactionInfoHelper,
                        fee = record.fee,
                        navController = navController
                    )
                }
            }
        }

//        uiState.itemSections.forEach { items ->
//            TransactionInfoSection(items, navController, { null })
//            VSpacer(12.dp)
//        }
    }
}
