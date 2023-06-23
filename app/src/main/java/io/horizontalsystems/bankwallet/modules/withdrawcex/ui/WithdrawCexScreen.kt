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
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.R
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.amount.AmountInputType
import cash.p.terminal.modules.amount.HSAmountInput
import cash.p.terminal.modules.availablebalance.AvailableBalance
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.FormsInputAddress
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.TextPreprocessorImpl
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

private val usd = Currency("USD", "$", 2, R.drawable.icon_32_flag_usa)
private val currencyValue = CurrencyValue(usd, BigDecimal.ONE)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WithdrawCexScreen(
    mainViewModel: WithdrawCexViewModel,
    onClose: () -> Unit,
    openNetworkSelect: () -> Unit,
    openConfirm: () -> Unit,
) {

    val focusRequester = remember { FocusRequester() }
    val navController = rememberAnimatedNavController()

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.CexWithdraw_Title, "ETH"),
                    navigationIcon = {
                        CoinImage(
                            iconUrl = "fullCoin.coin.imageUrl",
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
                    coinCode = "wallet.coin.code",
                    coinDecimal = 18,
                    fiatDecimal = 2,
                    availableBalance = BigDecimal.TEN,
                    amountInputType = AmountInputType.COIN,
                    rate = currencyValue,
                )
                VSpacer(8.dp)
                HSAmountInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    focusRequester = focusRequester,
                    availableBalance = BigDecimal.TEN,
                    caution = null,
                    coinCode = "wallet.coin.code",
                    coinDecimal = 18,
                    fiatDecimal = 2,
                    onClickHint = {
                        //amountInputModeViewModel.onToggleInputType()
                    },
                    onValueChange = {
                        // viewModel.onEnterAmount(it)
                    },
                    inputType = AmountInputType.COIN,
                    rate = currencyValue,
                    amountUnique = null
                )
                VSpacer(16.dp)
                NetworkInput(
                    title = "Ethereum",
                    onClick = openNetworkSelect
                )
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
                    //viewModel.onEnterAddress(it)
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
                    enabled = true
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