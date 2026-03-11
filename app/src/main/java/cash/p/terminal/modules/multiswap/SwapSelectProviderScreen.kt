package cash.p.terminal.modules.multiswap

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.HSCaution
import cash.p.terminal.modules.multiswap.action.ISwapProviderAction
import cash.p.terminal.modules.multiswap.providers.IMultiSwapProvider
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.modules.multiswap.settings.ISwapSetting
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HFillSpacer
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

@Composable
fun SwapSelectProviderScreen(
    onClickClose: () -> Unit,
    quotes: List<QuoteViewItem>,
    currentQuote: SwapProviderQuote?,
    swapRates: () -> Unit,
    onSelectQuote: (SwapProviderQuote) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.Swap_Providers),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Done),
                        onClick = onClickClose
                    )
                ),
            )
        },
        containerColor = ComposeAppTheme.colors.tyler,
    ) {
        LazyColumn(
            modifier = Modifier.padding(it),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                VSpacer(height = 4.dp)
            }
            items(quotes) { viewItem ->
                val borderColor = if (viewItem.quote.provider == currentQuote?.provider) {
                    ComposeAppTheme.colors.yellow50
                } else {
                    ComposeAppTheme.colors.steel20
                }

                ProviderItem(
                    borderColor = borderColor,
                    onSelectQuote = onSelectQuote,
                    viewItem = viewItem,
                    swapRates = swapRates
                )
            }

            item {
                VSpacer(height = 24.dp)
            }

        }
    }
}

@Composable
private fun ProviderItem(
    borderColor: Color,
    onSelectQuote: (SwapProviderQuote) -> Unit,
    viewItem: QuoteViewItem,
    swapRates: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    RowUniversal(
        modifier = Modifier
            .clip(shape)
            .clickable { onSelectQuote.invoke(viewItem.quote) }
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 16.dp),
    ) {
        val provider = viewItem.quote.provider
        Image(
            modifier = Modifier.size(32.dp),
            painter = painterResource(provider.icon),
            contentDescription = null
        )
        HSpacer(width = 16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            subhead2_leah(
                text = viewItem.tokenAmount,
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.End)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                body_leah(
                    text = provider.title,
                    textAlign = TextAlign.End
                )
                HFillSpacer(minWidth = 8.dp)
                viewItem.fiatAmount?.let { fiatAmount ->
                    Row {
                        subhead2_grey(text = fiatAmount, textAlign = TextAlign.End)
                        viewItem.diffWithFirst?.let { diff ->
                            HSpacer(width = 4.dp)
                            Text(
                                text = stringResource(
                                    R.string.Swap_FiatPriceImpact,
                                    diff.toPlainString()
                                ),
                                style = ComposeAppTheme.typography.subhead2,
                                color = getPriceImpactColor(PriceImpactLevel.Warning),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            ExchangeBlock(
                from = viewItem.rateFrom,
                to = viewItem.rateTo,
                swapRates = {
                    onSelectQuote.invoke(viewItem.quote)
                    swapRates()
                },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun ExchangeBlock(from: String, to: String, swapRates: () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = swapRates
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        subhead2_grey(
            text = from
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_swap3_20),
            contentDescription = "invert price",
            tint = ComposeAppTheme.colors.yellowD
        )
        subhead2_grey(
            text = to
        )
    }
}

@Preview
@Composable
private fun SwapSelectProviderScreenPreview() {
    val previewProvider = object : IMultiSwapProvider {
        override val id = "preview"
        override val title = "Uniswap V3"
        override val icon = R.drawable.uniswap_v3
        override val priority = 0
        override val walletUseCase get() = throw NotImplementedError()
        override val mevProtectionAvailable = false
        override suspend fun supports(token: Token) = true
        override suspend fun fetchQuote(
            tokenIn: Token,
            tokenOut: Token,
            amountIn: BigDecimal,
            settings: Map<String, Any?>
        ): ISwapQuote = throw NotImplementedError()

        override suspend fun fetchFinalQuote(
            tokenIn: Token,
            tokenOut: Token,
            amountIn: BigDecimal,
            swapSettings: Map<String, Any?>,
            sendTransactionSettings: SendTransactionSettings?,
            swapQuote: ISwapQuote
        ): ISwapFinalQuote = throw NotImplementedError()
    }

    val previewQuote = object : ISwapQuote {
        override val amountOut = BigDecimal("456.78")
        override val priceImpact: BigDecimal? = null
        override val fields = emptyList<DataField>()
        override val settings = emptyList<ISwapSetting>()
        override val tokenIn: Token get() = throw NotImplementedError()
        override val tokenOut: Token get() = throw NotImplementedError()
        override val amountIn = BigDecimal("0.1234")
        override val actionRequired: ISwapProviderAction? =
            null
        override val cautions = emptyList<HSCaution>()
    }

    val quote1 = SwapProviderQuote(provider = previewProvider, swapQuote = previewQuote)

    ComposeAppTheme(darkTheme = false) {
        SwapSelectProviderScreen(
            onClickClose = {},
            quotes = listOf(
                QuoteViewItem(
                    quote = quote1,
                    tokenAmount = "456.78 DAI",
                    fiatAmount = "$456.78",
                    diffWithFirst = null,
                    rateFrom = "1 ETH",
                    rateTo = "100 PIRATE"
                ),
                QuoteViewItem(
                    quote = quote1,
                    tokenAmount = "455.12 DAI",
                    fiatAmount = "$455.12",
                    diffWithFirst = BigDecimal("-1.66"),
                    rateFrom = "1 ETH",
                    rateTo = "99 PIRATE"
                ),
            ),
            currentQuote = quote1,
            onSelectQuote = {},
            swapRates = {}
        )
    }
}
