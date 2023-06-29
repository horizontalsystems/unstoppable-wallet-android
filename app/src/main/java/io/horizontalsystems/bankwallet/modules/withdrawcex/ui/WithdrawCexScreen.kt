package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.R
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.amount.AmountInputModeModule
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WithdrawCexScreen(
    mainViewModel: WithdrawCexViewModel,
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
    val navController = rememberAnimatedNavController()

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
            Column(modifier = Modifier.padding(it)) {
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
                    availableBalance = uiState.availableBalance ?: BigDecimal.ZERO,
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
                    VSpacer(16.dp)
                    NetworkInput(
                        title = networkName,
                        onClick = openNetworkSelect
                    )
                }
                VSpacer(16.dp)
                FormsInputAddress(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = null,
                    hint = stringResource(id = R.string.Watch_Address_Hint),
                    state = null,
                    textPreprocessor = TextPreprocessorImpl,
                    onChangeFocus = {
                        //isFocused = it
                    },
                    navController = navController,
                    chooseContactEnable = false,
                    blockchainType = BlockchainType.Ethereum,
                ) {
                    mainViewModel.onEnterAddress(it)
                }
                VSpacer(16.dp)
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
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = onClick
            ) {
                subhead2_grey(
                    text = stringResource(R.string.CexWithdraw_Network),
                    modifier = Modifier.weight(1f)
                )
                body_leah(
                    text = title,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
}