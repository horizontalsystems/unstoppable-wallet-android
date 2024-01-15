package io.horizontalsystems.bankwallet.modules.swapxxx

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapQuote
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_green50
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import java.math.BigDecimal

class SwapSelectProviderFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SwapSelectProviderScreen(navController)
    }
}

@Composable
fun SwapSelectProviderScreen(navController: NavController) {
    val viewModel = viewModel<SwapViewModel>(
        viewModelStoreOwner = navController.previousBackStackEntry!!,
        factory = SwapViewModel.Factory()
    )

    val uiState = viewModel.uiState

    SwapSelectProviderScreenInner(
        onClickClose = navController::popBackStack,
        quotes = uiState.quotes,
        preferredProviderId = uiState.preferredProvider?.id,
    ) {
        viewModel.onSelectQuote(it)
        navController.popBackStack()
    }
}

@Composable
private fun SwapSelectProviderScreenInner(
    onClickClose: () -> Unit,
    quotes: List<SwapProviderQuote>,
    preferredProviderId: String?,
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
                val borderColor = if (quote.provider.id == preferredProviderId) {
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
                        if (i == 0) {
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
private fun SwapSelectProviderScreenPreview() {
    ComposeAppTheme(darkTheme = false) {
        SwapSelectProviderScreenInner(
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
            preferredProviderId = SwapMainModule.OneInchProvider.id,
            onSelectQuote = {}
        )
    }
}
