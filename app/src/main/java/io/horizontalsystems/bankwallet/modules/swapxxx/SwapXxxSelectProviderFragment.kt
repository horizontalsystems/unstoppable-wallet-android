package cash.p.terminal.modules.swapxxx

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.SwapQuote
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HFillSpacer
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.subhead2_green50
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import java.math.BigDecimal

class SwapXxxSelectProviderFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapXxxSelectProviderScreen(navController)
    }
}

@Composable
fun SwapXxxSelectProviderScreen(navController: NavController) {
    val viewModel = viewModel<SwapXxxViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
        factory = SwapXxxViewModel.Factory()
    )

    val uiState = viewModel.uiState

    SwapXxxSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quotes,
        bestQuote = uiState.bestQuote,
        selectedQuote = uiState.selectedQuote,
    ) {
        viewModel.onSelectQuote(it)
        navController.popBackStack()
    }
}

@Composable
private fun SwapXxxSelectProviderScreenInner(
    onClickClose: () -> Unit,
    quotes: List<SwapProviderQuote>,
    bestQuote: SwapProviderQuote?,
    selectedQuote: SwapProviderQuote?,
    onSelectQuote: (SwapProviderQuote) -> Unit
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Done),
                        onClick = onClickClose
                    )
                ),
            )
        },
        backgroundColor = ComposeAppTheme.colors.tyler,
    ) {
        LazyColumn(
            modifier = Modifier.padding(it),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                VSpacer(height = 12.dp)
            }
            itemsIndexed(quotes) { i, quote ->
                val borderColor = if (quote == selectedQuote) {
                    ComposeAppTheme.colors.yellow50
                } else {
                    ComposeAppTheme.colors.steel20
                }

                RowUniversal(
                    modifier = Modifier
                        .padding(top = if (i == 0) 0.dp else 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    onClick = { onSelectQuote.invoke(quote) }
                ) {
                    val provider = quote.provider
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(provider.icon),
                        contentDescription = null
                    )
                    HSpacer(width = 16.dp)
                    Column {
                        subhead2_leah(
                            text = provider.title,
                            textAlign = TextAlign.End
                        )
                        if (bestQuote == quote) {
                            VSpacer(height = 1.dp)
                            subhead2_green50(
                                text = stringResource(R.string.Swap_BestPrice),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    HFillSpacer(minWidth = 8.dp)
                    Column(horizontalAlignment = Alignment.End) {
                        subhead2_leah(text = quote.quote.amountOut.toPlainString())
                        VSpacer(height = 1.dp)
                        subhead2_grey(text = "Fee")
                    }
                }
            }

            item {
                VSpacer(height = 24.dp)
            }

            item {
                TextImportantWarning(text = stringResource(R.string.Swap_SelectSwapProvider_FeeInfo))
            }

            item {
                VSpacer(height = 32.dp)
            }

        }
    }
}

@Preview
@Composable
private fun SwapXxxSelectProviderScreenPreview() {
    ComposeAppTheme(darkTheme = false) {
        SwapXxxSelectProviderScreenInner(
            onClickClose = {},
            quotes = listOf(
                SwapProviderQuote(
                    SwapMainModule.OneInchProvider,
                    SwapQuote(BigDecimal.TEN)
                ),
                SwapProviderQuote(
                    SwapMainModule.UniswapV3Provider,
                    SwapQuote(BigDecimal("10.12"))
                ),
            ),
            bestQuote = null,
            selectedQuote = null,
            onSelectQuote = {}
        )
    }
}
