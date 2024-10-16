package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imagePlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

@Composable
fun diffColor(value: BigDecimal?) : Color {
    val diff = value ?: BigDecimal.ZERO
    return when {
        diff.signum() == 0 -> ComposeAppTheme.colors.grey
        diff.signum() >= 0 -> ComposeAppTheme.colors.remus
        else -> ComposeAppTheme.colors.lucian
    }
}

@Composable
fun formatValueAsDiff(value: Value): String =
    App.numberFormatter.formatValueAsDiff(value)

@Composable
fun diffText(diff: BigDecimal?): String {
    if (diff == null) return ""
    val sign = when {
        diff == BigDecimal.ZERO -> ""
        diff >= BigDecimal.ZERO -> "+"
        else -> "-"
    }
    return App.numberFormatter.format(diff.abs(), 0, 2, sign, "%")
}

@Composable
fun CoinImage(
    coin: Coin?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = HsImage(
    url = coin?.imageUrl,
    alternativeUrl = coin?.alternativeImageUrl,
    placeholder = coin?.imagePlaceholder,
    modifier = modifier.clip(CircleShape),
    colorFilter = colorFilter
)

@Composable
fun CoinImage(
    token: Token?,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) = HsImageCircle(
    modifier,
    token?.coin?.imageUrl,
    token?.coin?.alternativeImageUrl,
    token?.iconPlaceholder,
    colorFilter
)

@Composable
fun HsImageCircle(
    modifier: Modifier,
    url: String?,
    alternativeUrl: String? = null,
    placeholder: Int? = null,
    colorFilter: ColorFilter? = null
) {
    HsImage(
        url = url,
        alternativeUrl = alternativeUrl,
        placeholder = placeholder,
        modifier = modifier.clip(CircleShape),
        colorFilter = colorFilter
    )
}

@Composable
fun HsImage(
    url: String?,
    alternativeUrl: String? = null,
    placeholder: Int? = null,
    modifier: Modifier,
    colorFilter: ColorFilter? = null
) {
    val fallback = placeholder ?: R.drawable.coin_placeholder
    when {
        url != null -> Image(
            painter = rememberAsyncImagePainter(
                model = url,
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