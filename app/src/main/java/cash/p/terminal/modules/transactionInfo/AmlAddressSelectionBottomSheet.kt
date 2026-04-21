package cash.p.terminal.modules.transactionInfo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.transactions.AmlStatus
import cash.p.terminal.modules.transactions.riskColor
import cash.p.terminal.modules.transactions.riskTextRes
import cash.p.terminal.ui_compose.components.HsRadioButton
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.D1
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmlAddressSelectionBottomSheet(
    addresses: List<Pair<String, AmlStatus>>,
    onAddressSelected: (String) -> Unit,
    onLaterClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAddress by remember { mutableStateOf<String?>(null) }

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        AmlAddressSelectionContent(
            addresses = addresses,
            selectedAddress = selectedAddress,
            onAddressClick = { selectedAddress = it },
            onCheckClick = {
                selectedAddress?.let { address ->
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onAddressSelected(address)
                    }
                }
            },
            onLaterClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onLaterClick()
                }
            },
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            }
        )
    }
}

@Composable
private fun AmlAddressSelectionContent(
    addresses: List<Pair<String, AmlStatus>>,
    selectedAddress: String?,
    onAddressClick: (String) -> Unit,
    onCheckClick: () -> Unit,
    onLaterClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_radar_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
        title = stringResource(R.string.address_checker_title),
        titleColor = ComposeAppTheme.colors.leah,
        onCloseClick = onCloseClick
    ) {
        Column {
            Spacer(Modifier.height(12.dp))
            HeaderText(
                text = stringResource(R.string.address_checker_choose_address)
            )
            Section(
                items = addresses,
                selectedItem = selectedAddress,
                onSelectListener = onAddressClick,
            )
            VSpacer(16.dp)

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.address_checker_title),
                enabled = selectedAddress != null,
                onClick = onCheckClick
            )

            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                title = stringResource(R.string.aml_check_info_later),
                onClick = onLaterClick
            )

            VSpacer(32.dp)
        }
    }
}

@Composable
private fun Section(
    items: List<Pair<String, AmlStatus>>,
    selectedItem: String?,
    onSelectListener: (String) -> Unit,
) {
    CellUniversalLawrenceSection(items, showFrame = true) { (address, amlStatus) ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                onSelectListener.invoke(address)
            },
        ) {
            HsRadioButton(
                selected = address == selectedItem,
                onClick = {
                    onSelectListener.invoke(address)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                body_leah(
                    text = address,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
                D1(
                    text = stringResource(amlStatus.riskTextRes),
                    textColor = amlStatus.riskColor()
                )
            }
        }
    }
}


@Preview
@Composable
private fun AmlAddressSelectionBottomSheetPreview() {
    val sampleAddresses = listOf(
        "0x1c6EAa67452C34C95206f5F7C7a6f76Ad81f51" to AmlStatus.Low,
        "0x2d7FBb78563D45D06317g8G8D87bBe92g62" to AmlStatus.Medium,
        "0x3e8GCc89674E56E17428h9H9E98cCf03h73" to AmlStatus.High,
    )
    ComposeAppTheme {
        AmlAddressSelectionContent(
            addresses = sampleAddresses,
            selectedAddress = sampleAddresses.first().first,
            onAddressClick = {},
            onCheckClick = {},
            onLaterClick = {},
            onCloseClick = {}
        )
    }
}
