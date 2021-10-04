package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.rememberImagePainter
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.getIconUrl
import io.horizontalsystems.bankwallet.core.getPlaceHolder
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

@Composable
fun RateColor(diff: BigDecimal?) =
    if (diff ?: BigDecimal.ZERO >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

@Composable
fun RateText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

@Composable
fun CoinImage(
    fullCoin: FullCoin,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) {
    Image(
        painter = rememberImagePainter(
            data = fullCoin.coin.getIconUrl(),
            builder = {
                error(fullCoin.getPlaceHolder())
            }
        ),
        contentDescription = "coin icon",
        modifier = modifier,
        colorFilter = colorFilter
    )
}