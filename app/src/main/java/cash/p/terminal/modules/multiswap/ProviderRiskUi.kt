package cash.p.terminal.modules.multiswap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.multiswap.providers.ProviderRiskType
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun ProviderRiskType.color(): Color = when (this) {
    ProviderRiskType.Auto -> ComposeAppTheme.colors.remus
    ProviderRiskType.Flexible -> ComposeAppTheme.colors.laguna
    ProviderRiskType.Controlled -> ComposeAppTheme.colors.jacob
    ProviderRiskType.PreCheck -> ComposeAppTheme.colors.bran
}

@Composable
internal fun BadgePill(
    iconRes: Int,
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = contentColor.copy(alpha = 0.1f),
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = ComposeAppTheme.typography.captionSB,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Tappable provider risk-type badge that opens the explanatory bottom sheet on click.
 * Self-contained so both the swap screen and the provider list can drop it in without
 * duplicating the sheet wiring.
 */
@Composable
fun ProviderRiskBadge(
    riskType: ProviderRiskType,
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }

    BadgePill(
        iconRes = riskType.iconRes,
        text = stringResource(riskType.titleRes),
        contentColor = riskType.color(),
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { showInfo = true },
        ),
    )

    if (showInfo) {
        ProviderRiskInfoBottomSheet(onDismiss = { showInfo = false })
    }
}

@Composable
fun EstimationTimeBadge(
    seconds: Long,
    modifier: Modifier = Modifier,
) {
    BadgePill(
        iconRes = R.drawable.clock_filled_24,
        text = formatDurationShort(seconds),
        contentColor = ComposeAppTheme.colors.grey,
        modifier = modifier,
        backgroundColor = ComposeAppTheme.colors.steel10,
    )
}

@Composable
@ReadOnlyComposable
fun formatDurationShort(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val parts = mutableListOf<String>()
    if (hours > 0) parts += stringResource(R.string.duration_short_hours, hours)
    if (minutes > 0) parts += stringResource(R.string.duration_short_minutes, minutes)
    if (seconds > 0 || (hours == 0L && minutes == 0L)) {
        parts += stringResource(R.string.duration_short_seconds, seconds)
    }
    return parts.joinToString(" ")
}

@Preview
@Composable
private fun ProviderRiskBadgesPreview() {
    ComposeAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProviderRiskType.entries.forEach { type ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstimationTimeBadge(seconds = 793)
                    ProviderRiskBadge(riskType = type)
                }
            }
        }
    }
}
