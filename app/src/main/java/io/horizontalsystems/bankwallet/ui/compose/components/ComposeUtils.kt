package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.DiffValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.math.BigDecimal

@Composable
fun RateColor(diff: BigDecimal?) =
    if (diff ?: BigDecimal.ZERO >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

@Composable
fun diffColor(diff: DiffValue) = when (diff) {
    is DiffValue.Negative -> ComposeAppTheme.colors.lucian
    is DiffValue.NoDiff,
    is DiffValue.Positive -> ComposeAppTheme.colors.remus
}

@Composable
fun RateText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

@Composable
fun CoinImage(
    iconUrl: String,
    placeholder: Int? = null ,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) {
    Image(
        painter = rememberImagePainter(
            data = iconUrl,
            builder = {
                error(placeholder ?: R.drawable.coin_placeholder)
            }
        ),
        contentDescription = "coin icon",
        modifier = modifier,
        colorFilter = colorFilter
    )
}