package io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRaw
import io.horizontalsystems.bankwallet.modules.hodler.HSHodlerInput
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.TransactionInputsSortInfoPage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
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

    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    ComposeAppTheme {
        HSScaffold(
            title = stringResource(R.string.Send_Advanced),
            onBack = navController::popBackStack,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    onClick = {
                        sendBitcoinViewModel.reset()
                        viewModel.reset()
                    },
                    tint = ComposeAppTheme.colors.jacob
                )
            )
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
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

                if (uiState.transactionSortingSupported) {
                    VSpacer(24.dp)
                    TransactionDataSortSettings(
                        navController,
                        wallet.coin.code,
                        viewModel.uiState.transactionSortTitle,
                    ) {
                        showBottomSheet = true
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
                            enabled = uiState.utxoExpertModeEnabled,
                            onChange = { viewModel.setUtxoExpertMode(it) }
                        )
                    }
                )
                InfoText(
                    text = stringResource(R.string.Send_Utxo_Description),
                )

                if (uiState.rbfVisible) {
                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            RbfSwitch(
                                enabled = uiState.rbfEnabled,
                                onChange = { viewModel.setRbfEnabled(it) }
                            )
                        }
                    )
                    InfoText(
                        text = stringResource(R.string.Send_Rbf_Description),
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
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState,
                    containerColor = ComposeAppTheme.colors.transparent
                ) {
                    BottomSheetTransactionOrderSelector(
                        items = uiState.transactionSortOptions,
                        onSelect = { mode ->
                            viewModel.setTransactionMode(mode)
                        },
                        onCloseClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }
                    )
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
                    headline2_leah(text = stringResource(item.mode.title))
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
    coinCode: String,
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
        text = stringResource(
            R.string.BtcBlockchainSettings_TransactionInputsOutputsSettingsDescription,
            coinCode
        ),
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
