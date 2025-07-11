package cash.p.terminal.modules.settings.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun SystemPinBlock(
    isPinRequired: Boolean,
    enabled: Boolean,
    showInfoBlock: Boolean = true,
    onPinRequiredChange: (Boolean) -> Unit,
) {
    CellUniversalLawrenceSection {
        SecurityCenterCell(
            start = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_passcode),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            },
            center = {
                if (enabled) {
                    body_leah(
                        text = stringResource(R.string.SettingsSecurity_system_pin),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    body_grey(
                        text = stringResource(R.string.SettingsSecurity_system_pin),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!isPinRequired) {
                    Image(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(20.dp),
                        painter = painterResource(id = R.drawable.ic_attention_red_20),
                        contentDescription = null,
                    )
                }
            },
            end = {
                HsSwitch(
                    checked = isPinRequired,
                    onCheckedChange = onPinRequiredChange,
                    enabled = enabled
                )
            }
        )
    }

    if (showInfoBlock) {
        InfoText(
            text = stringResource(R.string.SettingsSecurity_system_pin_description),
            paddingBottom = if (isPinRequired) 0.dp else 32.dp
        )
        if (isPinRequired) {
            InfoText(
                text = stringResource(R.string.SettingsSecurity_system_pin_description_enabled),
                paddingBottom = 32.dp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SystemPinBlockPreview() {
    ComposeAppTheme {
        Column {
            SystemPinBlock(
                enabled = true,
                isPinRequired = false,
                onPinRequiredChange = {}
            )
        }
    }
}
