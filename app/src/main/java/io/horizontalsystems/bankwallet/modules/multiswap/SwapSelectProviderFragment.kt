package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class SwapSelectProviderFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapSelectProviderScreen(navController)
    }
}

@Composable
fun SwapSelectProviderScreen(navController: NavController) {
    val previousBackStackEntry = remember { navController.previousBackStackEntry }
    val swapViewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = previousBackStackEntry!!,
    )
    val viewModel = viewModel<SwapSelectProviderViewModel>(
        factory = SwapSelectProviderViewModel.Factory(swapViewModel.uiState.quotes)
    )

    val uiState = viewModel.uiState
    val currentQuote = swapViewModel.uiState.quote

    SwapSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quoteViewItems,
        currentQuote = currentQuote,
    ) {
        swapViewModel.onSelectQuote(it)
        navController.popBackStack()

        stat(page = StatPage.SwapProvider, event = StatEvent.SwapSelectProvider(it.provider.id))
    }
}

@Composable
private fun SwapSelectProviderScreenInner(
    onClickClose: () -> Unit,
    quotes: List<QuoteViewItem>,
    currentQuote: SwapProviderQuote?,
    onSelectQuote: (SwapProviderQuote) -> Unit
) {
    HSScaffold(
        title = stringResource(R.string.Swap_Providers),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Done),
                onClick = onClickClose,
                tint = ComposeAppTheme.colors.jacob
            )
        ),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            item {
                VSpacer(height = 12.dp)
            }
            itemsIndexed(quotes) { i, viewItem ->
                val borderColor = if (viewItem.quote == currentQuote) {
                    ComposeAppTheme.colors.yellow50
                } else {
                    ComposeAppTheme.colors.blade
                }

                RowUniversal(
                    modifier = Modifier
                        .padding(top = if (i == 0) 0.dp else 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    onClick = { onSelectQuote.invoke(viewItem.quote) }
                ) {
                    val provider = viewItem.quote.provider
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
                    }
                    HFillSpacer(minWidth = 8.dp)
                    Column(horizontalAlignment = Alignment.End) {
                        subhead2_leah(
                            text = viewItem.tokenAmount,
                            textAlign = TextAlign.End
                        )
                        viewItem.fiatAmount?.let { fiatAmount ->
                            VSpacer(4.dp)
                            subhead2_grey(text = fiatAmount, textAlign = TextAlign.End)
                        }
                    }
                }
            }

            item {
                VSpacer(height = 32.dp)
            }

        }
    }
}

@Preview
@Composable
private fun SwapSelectProviderScreenPreview() {
    ComposeAppTheme(darkTheme = false) {
        SwapSelectProviderScreenInner(
            onClickClose = {},
            quotes = listOf(
//                SwapProviderQuote(
//                    SwapMainModule.OneInchProvider,
//                    quote.amountOut,
//                    quote.fee,
//                    quote.fields
//                ),
//                SwapProviderQuote(
//                    SwapMainModule.UniswapV3Provider,
//                    quote.amountOut,
//                    quote.fee,
//                    quote.fields
//                ),
            ),
            currentQuote = null,
            onSelectQuote = {}
        )
    }
}
