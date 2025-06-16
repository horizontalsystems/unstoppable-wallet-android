package cash.p.terminal.modules.settings.security.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.settings.security.SecurityCenterCell
import cash.p.terminal.ui.compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceMutableSection
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun TransferPasscodeBlock(
    transferPasscodeEnabled: Boolean,
    onTransferPasscodeEnabledChange: (Boolean) -> Unit,
) {
    CellUniversalLawrenceMutableSection(
        mutableListOf<@Composable () -> Unit>().apply {
            add {
                SecurityCenterCell(
                    start = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_off_24),
                            tint = ComposeAppTheme.colors.grey,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                    },
                    center = {
                        body_leah(
                            text = stringResource(id = R.string.appearance_transfer_passcode),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    end = {
                        HsSwitch(
                            checked = transferPasscodeEnabled,
                            onCheckedChange = onTransferPasscodeEnabledChange
                        )
                    }
                )
            }
        }
    )
    InfoText(
        text = stringResource(R.string.appearance_transfer_passcode_description),
        paddingBottom = 32.dp
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransferPasscodeBlockPreview() {
    var transferPasscodeEnabled by remember { mutableStateOf(true) }
    ComposeAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TransferPasscodeBlock(
                transferPasscodeEnabled = transferPasscodeEnabled,
                onTransferPasscodeEnabledChange = {
                    transferPasscodeEnabled = it
                },
            )
        }
    }
}
