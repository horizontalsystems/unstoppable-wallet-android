package cash.p.terminal.modules.settings.security.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.body_jacob
import cash.p.terminal.ui_compose.components.body_lucian
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun ManagePasscodeSection(
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    onManageClick: () -> Unit,
    onDisableClick: () -> Unit,
    @StringRes editTextRes: Int,
    @StringRes enableTextRes: Int = R.string.SettingsSecurity_EnablePin,
    @StringRes disableTextRes: Int = R.string.SettingsSecurity_DisablePin,
    showWarningWhenDisabled: Boolean = false,
) {
    CellUniversalLawrenceSection(buildList<@Composable () -> Unit> {
        add {
            SecurityCenterCell(
                start = {
                    Icon(
                        painter = painterResource(iconRes),
                        tint = ComposeAppTheme.colors.jacob,
                        modifier = Modifier.size(24.dp),
                        contentDescription = null,
                    )
                },
                center = {
                    body_jacob(
                        text = stringResource(if (enabled) editTextRes else enableTextRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                end = {
                    if (showWarningWhenDisabled && !enabled) {
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_attention_red_20),
                            contentDescription = null,
                        )
                    }
                },
                onClick = onManageClick
            )
        }
        if (enabled) {
            add {
                SecurityCenterCell(
                    start = {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_20),
                            tint = ComposeAppTheme.colors.lucian,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null,
                        )
                    },
                    center = {
                        body_lucian(
                            text = stringResource(disableTextRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = onDisableClick
                )
            }
        }
    })
}
