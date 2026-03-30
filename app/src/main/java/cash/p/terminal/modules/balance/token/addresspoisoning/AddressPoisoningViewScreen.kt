package cash.p.terminal.modules.balance.token.addresspoisoning

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.transactions.TransactionCell
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.ui_compose.ColorName
import cash.p.terminal.ui_compose.ColoredValue
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsCheckbox
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.entities.SectionItemPosition
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import java.util.Date

@Composable
internal fun AddressPoisoningViewScreen(
    uiState: AddressPoisoningViewUiState,
    onSelect: (AddressPoisoningViewMode) -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.address_poisoning_view),
                navigationIcon = { HsBackButton(onClick = onClose) },
            )
        },
        containerColor = ComposeAppTheme.colors.tyler,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            VSpacer(23.dp)
            subhead2_grey(
                text = stringResource(R.string.address_poisoning_view_select_style),
                modifier = Modifier.padding(horizontal = 26.dp),
            )
            VSpacer(20.dp)
            ModeOptionCard(
                label = stringResource(R.string.address_poisoning_view_standard),
                selected = uiState.selectedMode == AddressPoisoningViewMode.STANDARD,
                onClick = { onSelect(AddressPoisoningViewMode.STANDARD) },
            ) {
                TransactionCell(
                    item = uiState.standardItem,
                    position = SectionItemPosition.Single,
                    onValueClick = { onSelect(AddressPoisoningViewMode.STANDARD) },
                    onClick = { onSelect(AddressPoisoningViewMode.STANDARD) },
                )
            }
            VSpacer(12.dp)
            ModeOptionCard(
                label = stringResource(R.string.address_poisoning_view_compact),
                selected = uiState.selectedMode == AddressPoisoningViewMode.COMPACT,
                onClick = { onSelect(AddressPoisoningViewMode.COMPACT) },
            ) {
                TransactionCell(
                    item = uiState.compactItem,
                    position = SectionItemPosition.Single,
                    onValueClick = { onSelect(AddressPoisoningViewMode.COMPACT) },
                    onClick = { onSelect(AddressPoisoningViewMode.COMPACT) },
                )
            }
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun ModeOptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val borderColor = if (selected) {
        ComposeAppTheme.colors.jacob
    } else {
        ComposeAppTheme.colors.steel20
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HsCheckbox(checked = selected, onCheckedChange = { onClick() })
            HSpacer(16.dp)
            subhead2_leah(text = label)
        }
        VSpacer(26.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
        ) {
            preview()
        }
    }
}


@Suppress("UnusedPrivateMember")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddressPoisoningViewScreenPreview() {
    val mockItem = TransactionViewItem(
        uid = "preview",
        progress = null,
        title = "Sent",
        subtitle = "to xxxxxxxxxxxx...xxxx...xxxxxxxxxx",
        primaryValue = ColoredValue("-1 ETH", ColorName.Lucian),
        secondaryValue = ColoredValue("$3,198", ColorName.Grey),
        date = Date(),
        formattedTime = "14:30",
        icon = TransactionViewItem.Icon.Failed,
        poisonStatus = PoisonStatus.CREATED,
        addressPoisoningViewMode = AddressPoisoningViewMode.STANDARD,
    )
    ComposeAppTheme {
        AddressPoisoningViewScreen(
            uiState = AddressPoisoningViewUiState(
                selectedMode = AddressPoisoningViewMode.STANDARD,
                standardItem = mockItem,
                compactItem = mockItem.copy(
                    uid = "preview_compact",
                    addressPoisoningViewMode = AddressPoisoningViewMode.COMPACT,
                ),
            ),
            onSelect = {},
            onClose = {},
        )
    }
}
