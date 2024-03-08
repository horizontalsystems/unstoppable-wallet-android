package io.horizontalsystems.bankwallet.modules.swapxxx

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
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
                    if (uiState.refreshing) {
                        ButtonPrimaryYellow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            title = stringResource(R.string.Alert_Loading),
                            enabled = false,
                            onClick = { },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote expired")
                    } else if (uiState.expired) {
                        ButtonPrimaryYellow(
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
                            title = stringResource(R.string.Button_Confirm),
                            onClick = {
                                viewModel.swap()
                            },
                        )
                        VSpacer(height = 12.dp)
                        subhead1_leah(text = "Quote expires in ${uiState.expiresIn} seconds")
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
            VSpacer(height = 16.dp)
            SectionUniversalLawrence {
                val swapPriceUIHelper = SwapPriceUIHelper(uiState.tokenIn, uiState.tokenOut, uiState.amountIn, uiState.amountOut)
                SwapInfoRow(false, stringResource(id = R.string.Swap_Price), swapPriceUIHelper.priceStr)
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
    amount: BigDecimal,
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
                text = CoinValue(token, amount).getFormattedFull(),
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
