package io.horizontalsystems.walletkit.modules.multiswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.multiswap.pathfinder.SwapPath
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.uiv3.components.cell.CellLeftCoinIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CoinIconSize

/**
 * Shown under the swap form when the pair gets no quote but a one-hop route exists. Tapping the
 * highlighted first leg hands its intermediate token to [onSwapFirstHop] so the user can make that
 * swap right away.
 */
@Composable
fun SwapSuggestedPathCard(
    path: SwapPath,
    modifier: Modifier = Modifier,
    onSwapFirstHop: (Token) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.tyler)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            subhead1_leah(stringResource(R.string.Swap_SuggestedPath_NoQuotesAvailable))
            VSpacer(4.dp)
            Text(
                text = suggestedPathText(path, onSwapFirstHop),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            CellLeftCoinIcon(token = path.tokenIn, size = CoinIconSize.Compact)
            HopArrow()
            CellLeftCoinIcon(token = path.intermediateToken, size = CoinIconSize.Compact)
            HopArrow()
            CellLeftCoinIcon(token = path.tokenOut, size = CoinIconSize.Compact)
        }
    }
}

@Composable
private fun HopArrow() {
    HSpacer(8.dp)
    Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(R.drawable.arrow_m_right_24),
        contentDescription = null,
        tint = ComposeAppTheme.colors.grey,
    )
    HSpacer(8.dp)
}

/**
 * The sentence with its first leg rendered as a link. The translation is formatted with a marker
 * in the link's slot and split on it afterwards, so the link lands wherever the locale puts it.
 */
@Composable
private fun suggestedPathText(path: SwapPath, onSwapFirstHop: (Token) -> Unit): AnnotatedString {
    val marker = "\u0000"
    val sentence = stringResource(
        R.string.Swap_SuggestedPath_Text,
        marker,
        path.intermediateToken.coin.code,
        path.tokenOut.coin.code,
    )
    val firstHop = stringResource(
        R.string.Swap_SuggestedPath_TextPart,
        path.tokenIn.coin.code,
        path.intermediateToken.coin.code,
    )
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = ComposeAppTheme.colors.jacob,
            fontWeight = ComposeAppTheme.typography.captionSB.fontWeight,
            textDecoration = TextDecoration.Underline,
        )
    )

    return buildAnnotatedString {
        val parts = sentence.split(marker, limit = 2)
        append(parts[0])
        withLink(
            LinkAnnotation.Clickable(tag = "first_hop", styles = linkStyle) {
                onSwapFirstHop(path.intermediateToken)
            }
        ) {
            append(firstHop)
        }
        if (parts.size > 1) {
            append(parts[1])
        }
    }
}
