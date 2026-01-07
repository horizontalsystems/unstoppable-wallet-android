package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.paidAction
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.subscriptions.core.PrioritySupport

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
        factory = SwapSelectProviderViewModel.Factory(swapViewModel.uiState.quotes, swapViewModel.uiState.quote)
    )

    val uiState = viewModel.uiState

    SwapSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quoteViewItems,
        currentQuote = uiState.selectedQuote,
    ) {
        navController.paidAction(PrioritySupport) {
            swapViewModel.onSelectQuote(it)
            navController.popBackStack()
        }

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
        onBack = onClickClose,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence),
            ) {
                quotes.forEachIndexed { index, viewItem ->
                    val provider = viewItem.quote.provider
                    val icon = if (viewItem.quote == currentQuote) {
                        R.drawable.selector_checked_20
                    } else {
                        R.drawable.selector_unchecked_20
                    }
                    val iconTint = if (viewItem.quote == currentQuote) {
                        ComposeAppTheme.colors.jacob
                    } else {
                        ComposeAppTheme.colors.andy
                    }
                    if (index > 0) {
                        HsDivider()
                    }
                    CellPrimary(
                        left = {
                            Image(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(provider.icon),
                                contentDescription = null
                            )
                        },
                        middle = {
                            CellMiddleInfo(
                                subtitle = provider.title.hs,
                                description = "DEX".hs,
                            )
                        },
                        right = {
                            CellRightSelectors(
                                subtitle = viewItem.tokenAmount.hs,
                                description1 = viewItem.fiatAmount?.hs,
                                description2 = getPriceImpact(viewItem.priceImpactData),
                                icon = painterResource(icon),
                                iconTint = iconTint
                            )
                        },
                        onClick = { onSelectQuote.invoke(viewItem.quote) }
                    )
                }
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun getPriceImpact(priceImpactData: PriceImpactData?): HSString? {
    if (priceImpactData == null) {
        return null
    }
    val color = when (priceImpactData.priceImpactLevel) {
        PriceImpactLevel.Normal -> null
        PriceImpactLevel.Warning -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.lucian
    }
    val value = App.numberFormatter.format(priceImpactData.priceImpact, 0, 2, prefix = "-", suffix = "%")
    return "($value)".hs(color = color)
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
