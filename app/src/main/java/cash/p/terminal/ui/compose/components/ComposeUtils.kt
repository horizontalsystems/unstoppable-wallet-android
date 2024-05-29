package cash.p.terminal.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.alternativeImageUrl
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imagePlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.market.Value
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Composable
fun RateColor(diff: BigDecimal?) =
    if ((diff ?: BigDecimal.ZERO) >= BigDecimal.ZERO) ComposeAppTheme.colors.remus else ComposeAppTheme.colors.lucian

@Composable
fun diffColor(value: BigDecimal) =
    if (value.signum() >= 0) {
        ComposeAppTheme.colors.remus
    } else {
        ComposeAppTheme.colors.lucian
    }

@Composable
fun formatValueAsDiff(value: Value): String =
    App.numberFormatter.formatValueAsDiff(value)

@Composable
fun RateText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = if (diff >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

@Composable
fun CoinImage(
    coin: Coin?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = CoinImage(
    iconUrl = coin?.imageUrl,
    alternativeUrl = coin?.alternativeImageUrl,
    placeholder = coin?.imagePlaceholder,
    modifier = modifier,
    colorFilter = colorFilter
)

@Composable
fun CoinImage(
    token: Token?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = CoinImage(
    iconUrl = token?.coin?.imageUrl,
    alternativeUrl = token?.coin?.alternativeImageUrl,
    placeholder = token?.iconPlaceholder,
    modifier = modifier,
    colorFilter = colorFilter
)

@Composable
fun CoinImage(
    iconUrl: String?,
    alternativeUrl: String? = null,
    placeholder: Int? = null,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) {
    val fallback = placeholder ?: R.drawable.coin_placeholder
    when {
        iconUrl != null -> Image(
            painter = rememberAsyncImagePainter(
                model = iconUrl,
                error = alternativeUrl?.let {
                    rememberAsyncImagePainter(
                        model = alternativeUrl,
                        error = painterResource(fallback)
                    )
                } ?: painterResource(fallback)
            ),
            contentDescription = null,
            modifier = modifier,
            colorFilter = colorFilter,
            contentScale = ContentScale.FillBounds
        )

        else -> Image(
            painter = painterResource(fallback),
            contentDescription = null,
            modifier = modifier,
            colorFilter = colorFilter
        )
    }
}

@Composable
fun NftIcon(
    modifier: Modifier = Modifier,
    iconUrl: String?,
    placeholder: Int? = null,
    colorFilter: ColorFilter? = null
) {
    val fallback = placeholder ?: R.drawable.ic_platform_placeholder_24
    when {
        iconUrl != null -> Image(
            painter = rememberAsyncImagePainter(
                model = iconUrl,
                error = painterResource(fallback)
            ),
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .size(32.dp),
            colorFilter = colorFilter,
            contentScale = ContentScale.Crop
        )

        else -> Image(
            painter = painterResource(fallback),
            contentDescription = null,
            modifier = modifier.size(32.dp),
            colorFilter = colorFilter
        )
    }
}