package cash.p.terminal.modules.send.bitcoin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.address.AddressParserModule
import cash.p.terminal.modules.address.AddressParserViewModel
import cash.p.terminal.modules.address.HSAddressInput
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.fee.HSFeeRaw
import cash.p.terminal.modules.memo.HSMemoInput
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.send.SendConfirmationFragment
import cash.p.terminal.modules.send.bitcoin.advanced.BtcTransactionInputSortInfoScreen
import cash.p.terminal.modules.send.bitcoin.advanced.FeeRateCaution
import cash.p.terminal.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import cash.p.terminal.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import cash.p.terminal.modules.sendtokenselect.PrefilledData
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import java.math.BigDecimal


const val SendBtcPage = "send_btc"
const val SendBtcAdvancedSettingsPage = "send_btc_advanced_settings"
const val TransactionInputsSortInfoPage = "transaction_input_sort_info_settings"
const val UtxoExpertModePage = "utxo_expert_mode_page"

@Composable
fun SendBitcoinNavHost(
    title: String,
    fragmentNavController: NavController,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    prefilledData: PrefilledData?,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SendBtcPage,
    ) {
        composable(SendBtcPage) {
            SendBitcoinScreen(
                title = title,
                fragmentNavController = fragmentNavController,
                composeNavController = navController,
                viewModel = viewModel,
                amountInputModeViewModel = amountInputModeViewModel,
                sendEntryPointDestId = sendEntryPointDestId,
                prefilledData = prefilledData,
            )
        }
        composablePage(SendBtcAdvancedSettingsPage) {
            SendBtcAdvancedSettingsScreen(
                fragmentNavController = fragmentNavController,
                navController = navController,
                sendBitcoinViewModel = viewModel,
                amountInputType = amountInputModeViewModel.inputType,
            )
        }
        composablePopup(TransactionInputsSortInfoPage) { BtcTransactionInputSortInfoScreen { navController.popBackStack() } }
        composablePage(UtxoExpertModePage) {
            UtxoExpertModeScreen(
                adapter = viewModel.adapter,
                token = viewModel.wallet.token,
                customUnspentOutputs = viewModel.customUnspentOutputs,
                updateUnspentOutputs = {
                    viewModel.updateCustomUnspentOutputs(it)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun SendBitcoinScreen(
    title: String,
    fragmentNavController: NavController,
    composeNavController: NavController,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    prefilledData: PrefilledData?,
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    val availableBalance = uiState.availableBalance
    val addressError = uiState.addressError
    val amountCaution = uiState.amountCaution
    val fee = uiState.fee
    val proceedEnabled = uiState.canBeSend
    val amountInputType = amountInputModeViewModel.inputType
    val feeRateCaution = uiState.feeRateCaution

    val rate = viewModel.coinRate

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, prefilledData?.amount)
    )
    val amountUnique = paymentAddressViewModel.amountUnique

    ComposeAppTheme {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = { fragmentNavController.popBackStack() })
                },
                menuItems = if (uiState.isAdvancedSettingsAvailable) {
                    listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                            icon = R.drawable.ic_manage_2,
                            tint = ComposeAppTheme.colors.jacob,
                            onClick = { composeNavController.navigate(SendBtcAdvancedSettingsPage) }
                        ),
                    )
                } else {
                    emptyList()
                }
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (uiState.showAddressInput) {
                    HSAddressInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        initial = prefilledData?.address?.let { Address(it) },
                        tokenQuery = wallet.token.tokenQuery,
                        coinCode = wallet.coin.code,
                        error = addressError,
                        textPreprocessor = paymentAddressViewModel,
                        navController = fragmentNavController
                    ) {
                        viewModel.onEnterAddress(it)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                HSAmountInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    focusRequester = focusRequester,
                    availableBalance = availableBalance ?: BigDecimal.ZERO,
                    caution = amountCaution,
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    onClickHint = {
                        amountInputModeViewModel.onToggleInputType()
                    },
                    onValueChange = {
                        viewModel.onEnterAmount(it)
                    },
                    inputType = amountInputType,
                    rate = rate,
                    amountUnique = amountUnique
                )

                Spacer(modifier = Modifier.height(12.dp))
                AvailableBalance(
                    coinCode = wallet.coin.code,
                    coinDecimal = viewModel.coinMaxAllowedDecimals,
                    fiatDecimal = viewModel.fiatMaxAllowedDecimals,
                    availableBalance = availableBalance,
                    amountInputType = amountInputType,
                    rate = rate
                )


                if (uiState.isMemoAvailable) {
                    VSpacer(12.dp)
                    HSMemoInput(maxLength = 120) {
                        viewModel.onEnterMemo(it)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                CellUniversalLawrenceSection(
                    buildList {
                        uiState.utxoData?.let { utxoData ->
                            add {
                                UtxoCell(
                                    utxoData = utxoData,
                                    onClick = {
                                        composeNavController.navigate(UtxoExpertModePage)
                                    }
                                )
                            }
                        }
                        add {
                            HSFeeRaw(
                                coinCode = wallet.coin.code,
                                coinDecimal = viewModel.coinMaxAllowedDecimals,
                                fee = fee,
                                amountInputType = amountInputType,
                                rate = rate,
                                navController = fragmentNavController
                            )
                        }
                    }
                )

                feeRateCaution?.let {
                    FeeRateCaution(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        feeRateCaution = feeRateCaution
                    )
                }

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = {
                        fragmentNavController.authorizedAction(
                            ConfirmPinFragment.InputConfirm(
                                descriptionResId = R.string.Unlock_EnterPasscode,
                                pinType = PinType.TRANSFER
                            )
                        ) {
                            fragmentNavController.slideFromRight(
                                R.id.sendConfirmation,
                                SendConfirmationFragment.Input(
                                    SendConfirmationFragment.Type.Bitcoin,
                                    sendEntryPointDestId
                                )
                            )
                        }
                    },
                    enabled = proceedEnabled
                )
            }
        }
    }
}

@Composable
fun UtxoCell(
    utxoData: SendBitcoinModule.UtxoData,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick
    ) {
        subhead2_grey(
            text = stringResource(R.string.Send_Utxos),
            modifier = Modifier.weight(1f)
        )
        subhead2_leah(text = utxoData.value)
        HSpacer(8.dp)
        when (utxoData.type) {
            SendBitcoinModule.UtxoType.Auto -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            SendBitcoinModule.UtxoType.Manual -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            }

            null -> {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
