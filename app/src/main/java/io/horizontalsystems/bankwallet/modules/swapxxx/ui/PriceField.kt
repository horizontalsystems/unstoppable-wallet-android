package cash.p.terminal.modules.swapxxx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.swapxxx.QuoteInfoRow
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.compose.components.subhead2_leah
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode

data class PriceField(
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val amountOut: BigDecimal,
) : SwapDataField {

    @Composable
    override fun GetContent() {
        val price = amountOut.divide(amountIn, tokenOut.decimals, RoundingMode.HALF_EVEN).stripTrailingZeros()
        val priceStr = "${CoinValue(tokenIn, BigDecimal.ONE).getFormattedFull()} = ${CoinValue(tokenOut, price).getFormattedFull()}"
        var showRegularPrice by remember { mutableStateOf(true) }

        QuoteInfoRow(
            title = {
                subhead2_grey(text = stringResource(R.string.Swap_Price))
            },
            value = {
                subhead2_leah(
                    modifier = Modifier
                        .clickable {
                            showRegularPrice = !showRegularPrice
                        },
                    text = if (showRegularPrice) priceStr else priceStr
                )
                HSpacer(width = 8.dp)
                Box(modifier = Modifier.size(14.5.dp)) {
                    val progress = remember { Animatable(1f) }
//                    LaunchedEffect(uiState) {
//                        progress.animateTo(
//                            targetValue = 0f,
//                            animationSpec = tween(uiState.quoteLifetime.toInt(), easing = LinearEasing),
//                        )
//                    }

                    CircularProgressIndicator(
                        progress = 1f,
                        modifier = Modifier.size(14.5.dp),
                        color = ComposeAppTheme.colors.steel20,
                        strokeWidth = 1.5.dp
                    )
                    CircularProgressIndicator(
                        progress = progress.value,
                        modifier = Modifier
                            .size(14.5.dp)
                            .scale(scaleX = -1f, scaleY = 1f),
                        color = ComposeAppTheme.colors.jacob,
                        strokeWidth = 1.5.dp
                    )
                }

            }
        )

    }
}