package cash.p.terminal.modules.send.bitcoin.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import cash.p.terminal.R
import cash.p.terminal.core.HSCaution
import cash.p.terminal.entities.TransactionDataSortMode
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.evmfee.EvmSettingsInput
import cash.p.terminal.modules.fee.HSFeeRaw
import cash.p.terminal.modules.hodler.HSHodlerInput
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.bitcoin.TransactionInputsSortInfoPage
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.TextImportantError
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.RowUniversal
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun SendBtcAdvancedSettingsScreen(
    fragmentNavController: NavController,
    navController: NavHostController,
    sendBitcoinViewModel: SendBitcoinViewModel,
    amountInputType: AmountInputType,
) {

    val wallet = sendBitcoinViewModel.wallet
    val sendUiState = sendBitcoinViewModel.uiState
    val feeRateVisible = sendBitcoinViewModel.feeRateChangeable
    val rate = sendBitcoinViewModel.coinRate
    val blockchainType = sendBitcoinViewModel.blockchainType
    val lockTimeIntervals = sendBitcoinViewModel.lockTimeIntervals
    val lockTimeEnabled = sendBitcoinViewModel.isLockTimeEnabled
    val lockTimeInterval = sendUiState.lockTimeInterval
    val feeRate = sendUiState.feeRate
    val feeRateCaution = sendUiState.feeRateCaution

    val viewModel: SendBtcAdvancedSettingsViewModel =
        viewModel(factory = SendBtcAdvancedSettingsModule.Factory(blockchainType))

    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    ComposeAppTheme {
        ModalBottomSheetLayout(
            sheetState = modalBottomSheetState,
            sheetBackgroundColor = ComposeAppTheme.colors.transparent,
            sheetContent = {
                BottomSheetTransactionOrderSelector(
                    items = viewModel.uiState.transactionSortOptions,
                    onSelect = { mode ->
                        viewModel.setTransactionMode(mode)
                    },
                    onCloseClick = {
                        coroutineScope.launch {
                            modalBottomSheetState.hide()
                        }
                    }
                )
            },
        ) {
            Scaffold(
                containerColor = ComposeAppTheme.colors.tyler,
                topBar = {
                    AppBar(
                        title = stringResource(R.string.Send_Advanced),
                        navigationIcon = {
                            HsBackButton(onClick = { navController.popBackStack() })
                        },
                        menuItems = listOf(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Button_Reset),
                                onClick = {
                                    sendBitcoinViewModel.reset()
                                    viewModel.reset()
                                }
                            )
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {

                    VSpacer(12.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            HSFeeRaw(
                                coinCode = wallet.coin.code,
                                coinDecimal = sendBitcoinViewModel.coinMaxAllowedDecimals,
                                fee = sendUiState.fee,
                                amountInputType = amountInputType,
                                rate = rate,
                                navController = fragmentNavController
                            )
                        }
                    )

                    if (feeRateVisible) {
                        VSpacer(24.dp)
                        EvmSettingsInput(
                            title = stringResource(R.string.FeeSettings_FeeRate),
                            info = stringResource(R.string.FeeSettings_FeeRate_Info),
                            value = feeRate?.toBigDecimal() ?: BigDecimal.ZERO,
                            decimals = 0,
                            caution = feeRateCaution,
                            navController = fragmentNavController,
                            onValueChange = {
                                sendBitcoinViewModel.updateFeeRate(it.toInt())
                            },
                            onClickIncrement = {
                                sendBitcoinViewModel.incrementFeeRate()
                            },
                            onClickDecrement = {
                                sendBitcoinViewModel.decrementFeeRate()
                            }
                        )
                        InfoText(
                            text = stringResource(R.string.FeeSettings_FeeRate_RecommendedInfo),
                        )
                    }

                    VSpacer(24.dp)
                    TransactionDataSortSettings(
                        navController,
                        viewModel.uiState.transactionSortTitle,
                    ) {
                        coroutineScope.launch {
                            modalBottomSheetState.show()
                        }
                    }

                    if (lockTimeEnabled) {
                        VSpacer(32.dp)
                        CellUniversalLawrenceSection(
                            listOf {
                                HSHodlerInput(
                                    lockTimeIntervals = lockTimeIntervals,
                                    lockTimeInterval = lockTimeInterval,
                                    onSelect = {
                                        sendBitcoinViewModel.onEnterLockTimeInterval(it)
                                    }
                                )
                            }
                        )
                        InfoText(
                            text = stringResource(R.string.Send_Hodler_Description),
                        )
                    }

                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            UtxoSwitch(
                                enabled = viewModel.uiState.utxoExpertModeEnabled,
                                onChange = { viewModel.setUtxoExpertMode(it) }
                            )
                        }
                    )
                    InfoText(
                        text = stringResource(R.string.Send_Utxo_Description),
                    )

                    VSpacer(32.dp)
                    CellUniversalLawrenceSection {
                        RbfSwitch(
                            enabled = viewModel.uiState.rbfEnabled,
                            onChange = { viewModel.setRbfEnabled(it) }
                        )
                    }

                    InfoText(
                        text = stringResource(R.string.Send_Rbf_Description),
                    )

                    feeRateCaution?.let {
                        FeeRateCaution(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp
                            ),
                            feeRateCaution = it
                        )
                    }

                    VSpacer(32.dp)
                }
            }
        }
    }
}

@Composable
fun UtxoSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    RowUniversal(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onChange.invoke(!enabled) }
        ),
    ) {
        body_leah(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_UtxoExpertMode),
        )
        Spacer(modifier = Modifier.weight(1f))
        HsSwitch(
            modifier = Modifier.padding(end = 16.dp),
            checked = enabled,
            onCheckedChange = { onChange.invoke(it) }
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun RbfSwitch(enabled: Boolean, onChange: (Boolean) -> Unit) {
    RowUniversal(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onChange.invoke(!enabled) }
        ),
    ) {
        body_leah(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_Rbf),
        )
        Spacer(modifier = Modifier.weight(1f))
        HsSwitch(
            modifier = Modifier.padding(end = 16.dp),
            checked = enabled,
            onCheckedChange = { onChange.invoke(it) }
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun BottomSheetTransactionOrderSelector(
    items: List<SendBtcAdvancedSettingsModule.SortModeViewItem>,
    onSelect: (TransactionDataSortMode) -> Unit,
    onCloseClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_arrow_up_right_12),
        title = stringResource(R.string.BtcBlockchainSettings_TransactionSettings),
        onCloseClick = onCloseClick,
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey)
    ) {
        Spacer(Modifier.height(12.dp))
        CellUniversalLawrenceSection(items, showFrame = true) { item ->
            RowUniversal(
                onClick = {
                    onSelect.invoke(item.mode)
                    onCloseClick.invoke()
                },
            ) {
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    body_leah(text = stringResource(item.mode.title))
                    subhead2_grey(text = stringResource(item.mode.description))
                }
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkmark_20),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(44.dp))
    }
}

@Composable
private fun TransactionDataSortSettings(
    navController: NavController,
    valueTitle: String,
    onClick: () -> Unit
) {
    HeaderText(
        text = stringResource(R.string.BtcBlockchainSettings_TransactionSettings),
        onInfoClick = {
            navController.navigate(TransactionInputsSortInfoPage)
        })
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal() {
                body_leah(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    text = stringResource(R.string.BtcBlockchainSettings_InputsOutputs)
                )
                ButtonSecondaryWithIcon(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .height(28.dp),
                    title = valueTitle,
                    iconRight = painterResource(R.drawable.ic_down_arrow_20),
                    onClick = onClick
                )
            }
        }
    )
    InfoText(
        text = stringResource(R.string.BtcBlockchainSettings_TransactionInputsOutputsSettingsDescription),
    )
}

@Composable
fun FeeRateCaution(modifier: Modifier, feeRateCaution: HSCaution) {
    when (feeRateCaution.type) {
        HSCaution.Type.Error -> {
            TextImportantError(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }

        HSCaution.Type.Warning -> {
            TextImportantWarning(
                modifier = modifier,
                icon = R.drawable.ic_attention_20,
                title = feeRateCaution.getString(),
                text = feeRateCaution.getDescription() ?: ""
            )
        }
    }
}
