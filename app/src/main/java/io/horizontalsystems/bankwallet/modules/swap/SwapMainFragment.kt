package io.horizontalsystems.bankwallet.modules.swap

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.PriceImpactLevel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun getPriceImpactColor(
    priceImpactLevel: PriceImpactLevel?
): Color {
    return when (priceImpactLevel) {
        PriceImpactLevel.Normal -> ComposeAppTheme.colors.jacob
        PriceImpactLevel.Warning,
        PriceImpactLevel.Forbidden -> ComposeAppTheme.colors.lucian

        else -> ComposeAppTheme.colors.grey
    }
}
