package cash.p.terminal.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.receive.viewmodels.MoneroSubaddressParcelable
import cash.p.terminal.modules.receive.viewmodels.MoneroUsedAddressesParams
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun MoneroUsedAddressesScreen(
    params: MoneroUsedAddressesParams,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Balance_Receive_UsedAddresses),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InfoText(text = stringResource(R.string.receive_monero_used_addresses_description))

            VSpacer(12.dp)

            CellUniversalLawrenceSection(
                buildList {
                    for (item in params.subaddresses) {
                        add {
                            MoneroAddressRow(item)
                        }
                    }
                }
            )

            VSpacer(24.dp)
        }
    }
}

@Composable
private fun MoneroAddressRow(item: MoneroSubaddressParcelable) {
    val view = LocalView.current
    val badgeText: String
    val badgeColor: Color

    if (item.receivedAmount > 0) {
        badgeText = stringResource(R.string.receive_address_badge_used)
        badgeColor = ComposeAppTheme.colors.jacob
    } else {
        badgeText = stringResource(R.string.receive_address_badge_unused)
        badgeColor = ComposeAppTheme.colors.grey
    }

    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                subhead2_grey(text = (item.index + 1).toString())
                HSpacer(4.dp)
                AddressBadgeChip(text = badgeText, color = badgeColor)
            }
            VSpacer(4.dp)
            subhead2_leah(
                text = item.address,
                modifier = Modifier.padding(start = 12.dp),
                textAlign = TextAlign.Start,
            )
        }

        HSpacer(16.dp)
        ButtonSecondaryCircle(
            icon = R.drawable.ic_copy_20,
            onClick = {
                TextHelper.copyText(item.address)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun MoneroUsedAddressesScreenPreview() {
    ComposeAppTheme {
        MoneroUsedAddressesScreen(
            params = MoneroUsedAddressesParams(
                subaddresses = listOf(
                    MoneroSubaddressParcelable(0, "48ZznXR8cxefi7b88Yf6MsYkENAhA1Aty", 150000000000L),
                    MoneroSubaddressParcelable(1, "89ABcDEF1234567890abcdef12345678ab", 0L),
                )
            ),
            onBackPress = {}
        )
    }
}

@Composable
fun AddressBadgeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = ComposeAppTheme.typography.captionSB,
        color = color,
    )
}
