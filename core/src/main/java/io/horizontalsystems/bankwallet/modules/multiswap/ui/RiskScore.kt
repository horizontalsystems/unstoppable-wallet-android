package io.horizontalsystems.bankwallet.modules.multiswap.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.multiswap.providers.RiskLevel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer

@Composable
fun riskLevelColor(riskLevel: RiskLevel): Color = when (riskLevel) {
    RiskLevel.EXCELLENT -> ComposeAppTheme.colors.remus
    RiskLevel.GOOD -> ComposeAppTheme.colors.ocean
    RiskLevel.FAIR -> ComposeAppTheme.colors.jacob
}

@Composable
fun RiskScore(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
) {
    val color = riskLevelColor(riskLevel)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(riskLevel.title),
            style = ComposeAppTheme.typography.subheadSB,
            color = color,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        HSpacer(4.dp)
        Icon(
            painter = painterResource(riskLevel.icon),
            modifier = Modifier.size(20.dp),
            tint = color,
            contentDescription = null
        )
    }
}
