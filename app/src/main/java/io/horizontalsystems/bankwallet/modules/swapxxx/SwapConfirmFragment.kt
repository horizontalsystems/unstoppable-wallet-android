package cash.p.terminal.modules.swapxxx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.badge
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.Cautions
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.caption_grey
import cash.p.terminal.ui.compose.components.cell.CellUniversal
import cash.p.terminal.ui.compose.components.cell.SectionUniversalLawrence
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SwapConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapConfirmScreen(navController)
    }
}

@Composable
fun SwapConfirmScreen(navController: NavController) {
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
        factory = SwapViewModel.Factory()
    )

    val currentQuote = remember { swapViewModel.getCurrentQuote() } ?: return
    val settings = remember { swapViewModel.getSettings() }

    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    val viewModel = viewModel<SwapConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry!!,
        initializer = SwapConfirmViewModel.init(currentQuote, settings)
    )

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Confirm_Title),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Settings_Title),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = {
                            navController.slideFromRight(R.id.swapTransactionSettings)
                        }
                    )
                ),
            )
        },
        bottomBar = {
            ButtonsGroupWithShade {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.loading) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Alert_Loading),
                            enabled = false,
                            onClick = { },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Loading final quote")
                    } else if (!uiState.validQuote) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Button_Refresh),
                            onClick = {
                                viewModel.refresh()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote is invalid")
                    } else if (uiState.expired) {
                        ButtonPrimaryDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Button_Refresh),
                            onClick = {
                                viewModel.refresh()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote expired")
                    } else {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Swap),
                            onClick = {
                                viewModel.swap()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote expires in ${uiState.expiresIn}")
                    }
                }
            }
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)
            SectionUniversalLawrence {
                TokenRow(uiState.tokenIn, uiState.amountIn, uiState.fiatAmountIn, uiState.currency, TokenRowType.In, false,)
                TokenRow(uiState.tokenOut, uiState.amountOut, uiState.fiatAmountOut, uiState.currency, TokenRowType.Out)
            }
//            VSpacer(height = 16.dp)
//            SectionUniversalLawrence {
//                val swapPriceUIHelper = SwapPriceUIHelper(uiState.tokenIn, uiState.tokenOut, uiState.amountIn, uiState.amountOut)
//                SwapInfoRow(false, stringResource(id = R.string.Swap_Price), swapPriceUIHelper.priceStr)
//            }
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                SwapInfoRow(
                    borderTop = false,
                    title = stringResource(id = R.string.FeeSettings_NetworkFee),
                    value = uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                    subvalue = uiState.networkFee?.secondary?.getFormattedPlain() ?: "---"
                )
            }

            if (uiState.cautions.isNotEmpty()) {
                Cautions(cautions = uiState.cautions)
            }

            VSpacer(height = 32.dp)
        }
    }
}

@Composable
private fun SwapInfoRow(borderTop: Boolean, title: String, value: String, subvalue: String? = null) {
    CellUniversal(borderTop = borderTop) {
        subhead2_grey(text = title)
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            subhead1_leah(text = value)
            subvalue?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = it)
            }
        }
    }
}

enum class TokenRowType {
    In, Out;
}

@Composable
private fun TokenRow(
    token: Token,
    amount: BigDecimal?,
    fiatAmount: BigDecimal?,
    currency: Currency,
    type: TokenRowType,
    borderTop: Boolean = true,
) {
    CellUniversal(borderTop = borderTop) {
        CoinImage(
            iconUrl = token.coin.imageUrl,
            placeholder = token.iconPlaceholder,
            modifier = Modifier.size(32.dp)
        )
        HSpacer(width = 16.dp)
        Column {
            val title = when (type) {
                TokenRowType.In -> stringResource(R.string.Send_Confirmation_YouSend)
                TokenRowType.Out -> stringResource(R.string.Swap_ToAmountTitle)
            }

            subhead2_leah(text = title)
            token.badge?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = it)
            }
        }
        HFillSpacer(minWidth = 8.dp)
        Column(horizontalAlignment = Alignment.End) {
            val color = when (type) {
                TokenRowType.In -> ComposeAppTheme.colors.leah
                TokenRowType.Out -> ComposeAppTheme.colors.remus
            }
            Text(
                text = amount?.let { CoinValue(token, it).getFormattedFull() } ?: "---",
                style = ComposeAppTheme.typography.subhead1,
                color = color,
            )
            fiatAmount?.let {
                VSpacer(height = 1.dp)
                caption_grey(text = CurrencyValue(currency, fiatAmount).getFormattedFull())
            }
        }
    }
}
