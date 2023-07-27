package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.amount.AmountInputModeModule
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.fee.FeeCell
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.FormsInputAddress
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.TextPreprocessorImpl
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import java.math.BigDecimal

@Composable
fun WithdrawCexScreen(
    mainViewModel: WithdrawCexViewModel,
    fragmentNavController: NavController,
    onClose: () -> Unit,
    openNetworkSelect: () -> Unit,
    openConfirm: () -> Unit,
) {
    val amountInputModeViewModel = mainViewModel.cexAsset.coin?.uid?.let {
        viewModel<AmountInputModeViewModel>(factory = AmountInputModeModule.Factory(it))
    }

    val amountInputType = amountInputModeViewModel?.inputType ?: AmountInputType.COIN
    val cexAsset = mainViewModel.cexAsset
    val uiState = mainViewModel.uiState

    val focusRequester = remember { FocusRequester() }

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.CexWithdraw_Title, cexAsset.id),
                    navigationIcon = {
                        CoinImage(
                            iconUrl = cexAsset.coin?.imageUrl,
                            placeholder = R.drawable.coin_placeholder,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                    },
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(rememberScrollState())
            ) {
                AvailableBalance(
                    coinCode = cexAsset.id,
                    coinDecimal = mainViewModel.coinMaxAllowedDecimals,
                    fiatDecimal = mainViewModel.fiatMaxAllowedDecimals,
                    availableBalance = uiState.availableBalance,
                    amountInputType = amountInputType,
                    rate = mainViewModel.coinRate,
                )
                VSpacer(8.dp)
                HSAmountInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    focusRequester = focusRequester,
                    availableBalance = uiState.availableBalance?.stripTrailingZeros() ?: BigDecimal.ZERO,
                    caution = uiState.amountCaution,
                    coinCode = cexAsset.id,
                    coinDecimal = mainViewModel.coinMaxAllowedDecimals,
                    fiatDecimal = mainViewModel.fiatMaxAllowedDecimals,
                    onClickHint = {
                        amountInputModeViewModel?.onToggleInputType()
                    },
                    onValueChange = {
                        mainViewModel.onEnterAmount(it)
                    },
                    inputType = amountInputType,
                    rate = mainViewModel.coinRate,
                    amountUnique = null
                )
                uiState.networkName?.let { networkName ->
                    VSpacer(12.dp)
                    NetworkInput(
                        title = networkName,
                        networkSelectionEnabled = mainViewModel.networkSelectionEnabled,
                        onClick = openNetworkSelect
                    )
                }
                VSpacer(12.dp)
                FormsInputAddress(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    value = mainViewModel.value,
                    hint = stringResource(id = R.string.Watch_Address_Hint),
                    state = uiState.addressState,
                    textPreprocessor = TextPreprocessorImpl,
                    navController = fragmentNavController,
                    chooseContactEnable = mainViewModel.hasContacts(),
                    blockchainType = mainViewModel.blockchainType,
                ) {
                    mainViewModel.onEnterAddress(it)
                }
                VSpacer(12.dp)
                CellUniversalLawrenceSection(
                    listOf(
                        {
                            FeeCell(
                                title = stringResource(R.string.CexWithdraw_Fee),
                                info = "",
                                value = uiState.feeItem,
                                viewState = null,
                                navController = null
                            )
                        },
                        {
                            RowUniversal(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                subhead2_grey(
                                    text = stringResource(id = R.string.CexWithdraw_FeeFromAmount),
                                    modifier = Modifier.weight(1f)
                                )

                                HsSwitch(
                                    checked = uiState.feeFromAmount,
                                    onCheckedChange = {
                                        mainViewModel.onSelectFeeFromAmount(it)
                                    }
                                )
                            }
                        }
                    )
                )
                VSpacer(12.dp)
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.CexWithdraw_NetworkDescription),
                )
                VSpacer(24.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.Send_DialogProceed),
                    onClick = {
                        openConfirm.invoke()
                    },
                    enabled = uiState.canBeSend
                )
                VSpacer(24.dp)
            }
        }
    }
}

@Composable
private fun NetworkInput(
    title: String,
    networkSelectionEnabled: Boolean,
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = if (networkSelectionEnabled) onClick else null
            ) {
                subhead2_grey(
                    text = stringResource(R.string.CexWithdraw_Network),
                    modifier = Modifier.weight(1f)
                )
                body_leah(
                    text = title,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                if (networkSelectionEnabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_down_arrow_20),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.grey
                    )
                }
            }
        }
    )
}