package cash.p.terminal.modules.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun AmlCheckPromoBanner(
    amlCheckEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ComposeAppTheme.colors.jacob, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star_filled_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob,
                modifier = Modifier.size(20.dp)
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.premium_title),
                color = ComposeAppTheme.colors.jacob,
                style = ComposeAppTheme.typography.subhead1
            )
            HsIconButton(
                modifier = Modifier.size(20.dp),
                onClick = onClose
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.alpha_aml_title),
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.body
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 20.dp),
                        onClick = onInfoClick
                    )
            )
            Spacer(Modifier.weight(1f))
            HsSwitch(
                checked = amlCheckEnabled,
                onCheckedChange = onToggleChange
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AmlCheckPromoBannerPreview() {
    ComposeAppTheme {
        AmlCheckPromoBanner(
            amlCheckEnabled = false,
            onToggleChange = {},
            onInfoClick = {},
            onClose = {}
        )
    }
}
