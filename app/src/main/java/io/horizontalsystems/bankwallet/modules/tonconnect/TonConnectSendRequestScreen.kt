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
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.xtransaction.TransactionInfoHelper
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxAmount
import io.horizontalsystems.bankwallet.modules.xtransaction.XxxSendReceiveSection
import io.horizontalsystems.bankwallet.modules.xtransaction.coinIconPainter
import io.horizontalsystems.bankwallet.modules.xtransaction.xxxCoinAmount
import io.horizontalsystems.bankwallet.modules.xtransaction.xxxFiatAmount
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
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
                                Text("TODO")
                            }

                            is TonTransactionRecord.Action.Type.ContractCall -> {
                                Text("TODO")
                            }

                            is TonTransactionRecord.Action.Type.ContractDeploy -> {
                                Text("TODO")
                            }

                            is TonTransactionRecord.Action.Type.Mint -> {
                                val transactionValue = actionType.value

                                SectionUniversalLawrence {
                                    XxxAmount(
                                        title = stringResource(R.string.Send_Confirmation_Mint),
                                        coinIcon = coinIconPainter(
                                            url = transactionValue.coinIconUrl,
                                            alternativeUrl = transactionValue.alternativeCoinIconUrl,
                                            placeholder = transactionValue.coinIconPlaceholder
                                        ),
                                        coinProtocolType = transactionValue.badge
                                            ?: stringResource(id = R.string.CoinPlatforms_Native),
                                        coinAmount = xxxCoinAmount(
                                            value = transactionValue.decimalValue?.abs(),
                                            coinCode = transactionValue.coinCode,
                                            sign = "+"
                                        ),
                                        coinAmountColor = ComposeAppTheme.colors.remus,
                                        fiatAmount = xxxFiatAmount(
                                            value = transactionInfoHelper.getXRate(transactionValue.coinUid)
                                                ?.let {
                                                    transactionValue.decimalValue?.abs()
                                                        ?.multiply(it)
                                                },
                                            fiatSymbol = transactionInfoHelper.getCurrencySymbol()
                                        ),
                                        onClick = {
                                            navController.slideFromRight(
                                                R.id.coinFragment,
                                                CoinFragment.Input(transactionValue.coinUid)
                                            )

                                            stat(
                                                page = StatPage.TonConnect,
                                                event = StatEvent.OpenCoin(transactionValue.coinUid)
                                            )
                                        },
                                        borderTop = false
                                    )
                                }
                            }

                            is TonTransactionRecord.Action.Type.Receive -> {
                                XxxSendReceiveSection(
                                    transactionValue = actionType.value,
                                    amountTitle = stringResource(R.string.Send_Confirmation_YouReceive),
                                    sign = "+",
                                    coinAmountColor = ComposeAppTheme.colors.remus,
                                    navController = navController,
                                    address = actionType.from,
                                    comment = actionType.comment,
                                    addressTitle = stringResource(R.string.TransactionInfo_From),
                                    addressStatSection = StatSection.AddressFrom,
                                    helper = transactionInfoHelper
                                )
                            }

                            is TonTransactionRecord.Action.Type.Send -> {
                                XxxSendReceiveSection(
                                    transactionValue = actionType.value,
                                    amountTitle = stringResource(R.string.Send_Confirmation_YouSend),
                                    sign = if (actionType.sentToSelf) "" else "-",
                                    coinAmountColor = ComposeAppTheme.colors.lucian,
                                    navController = navController,
                                    address = actionType.to,
                                    comment = actionType.comment,
                                    addressTitle = stringResource(R.string.TransactionInfo_To),
                                    addressStatSection = StatSection.AddressTo,
                                    helper = transactionInfoHelper
                                )
                            }

                            is TonTransactionRecord.Action.Type.Swap -> {
//                                Section {
//                                    Amount(
//                                        title = stringResource(R.string.TransactionInfo_YouSent),
//                                        coinValue = BigDecimal.ONE,
//                                        coinCode = "ETH",
//                                        coinIcon = painterResource(R.drawable.btc),
//                                        fiatValue = BigDecimal.ONE,
//                                        fiatSymbol = "$",
//                                    )
//                                    Amount(
//                                        title = stringResource(R.string.TransactionInfo_YouGot),
//                                    )
//                                }
                                Text("TODO")

                            }

                            is TonTransactionRecord.Action.Type.Unsupported -> {
                                Text("TODO")
                            }
                        }

                    }
                }
            }
        }

//        uiState.itemSections.forEach { items ->
//            TransactionInfoSection(items, navController, { null })
//            VSpacer(12.dp)
//        }
    }
}
