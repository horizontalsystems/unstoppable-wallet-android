package cash.p.terminal.modules.receive

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.receive.ui.AddressBadgeChip
import cash.p.terminal.modules.receive.ui.ReceiveAddressScreen
import cash.p.terminal.modules.receive.viewmodels.AddressBadge
import cash.p.terminal.modules.receive.viewmodels.MoneroSubaddressParcelable
import cash.p.terminal.modules.receive.viewmodels.MoneroUsedAddressesParams
import cash.p.terminal.modules.receive.viewmodels.ReceiveMoneroUiState
import cash.p.terminal.modules.receive.viewmodels.ReceiveMoneroViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsRadioButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Wallet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReceiveMoneroScreen(
    navController: NavController,
    wallet: Wallet,
    receiveEntryPointDestId: Int,
) {
    val viewModel: ReceiveMoneroViewModel = koinViewModel { parametersOf(wallet) }
    val uiState = viewModel.uiState
    var showConfirmDialog by remember { mutableStateOf(false) }

    ReceiveAddressScreen(
        title = stringResource(R.string.Deposit_Title, wallet.coin.code),
        uiState = uiState,
        setAmount = viewModel::setAmount,
        onErrorClick = viewModel::onErrorClick,
        topContent = { MoneroTopContent(uiState, viewModel, navController) },
        addressBadge = { MoneroAddressBadge(uiState.addressBadge) },
        bottomContent = {
            MoneroBottomContent(uiState, viewModel) { showConfirmDialog = true }
        },
        onBackPress = navController::navigateUp,
        closeModule = {
            if (receiveEntryPointDestId == 0) {
                navController.navigateUp()
            } else {
                navController.popBackStack(receiveEntryPointDestId, true)
            }
        }
    )

    if (showConfirmDialog) {
        ConfirmNewAddressBottomSheet(
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    viewModel.setSkipNewAddressConfirm(true)
                }
                viewModel.createNewAddress()
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun MoneroTopContent(
    uiState: ReceiveMoneroUiState,
    viewModel: ReceiveMoneroViewModel,
    navController: NavController,
) {
    UsedAddressesRow(
        enabled = uiState.hasAddressHistory,
        onClick = if (uiState.hasAddressHistory) {
            {
                val params = MoneroUsedAddressesParams(
                    subaddresses = viewModel.getSubaddressesForHistory().map {
                        MoneroSubaddressParcelable(it.index, it.address, it.receivedAmount)
                    }
                )
                navController.slideFromRight(
                    R.id.moneroUsedAddressesFragment,
                    params
                )
            }
        } else null
    )
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = ComposeAppTheme.colors.steel20,
    )
}

@Composable
private fun MoneroAddressBadge(badge: AddressBadge) {
    val badgeText = when (badge) {
        AddressBadge.NEW -> stringResource(R.string.receive_address_badge_new)
        AddressBadge.USED -> stringResource(R.string.receive_address_badge_used)
        AddressBadge.UNUSED -> stringResource(R.string.receive_address_badge_unused)
    }
    val badgeColor = when (badge) {
        AddressBadge.NEW -> ComposeAppTheme.colors.remus
        AddressBadge.USED -> ComposeAppTheme.colors.jacob
        AddressBadge.UNUSED -> ComposeAppTheme.colors.grey
    }
    VSpacer(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
    ) {
        AddressBadgeChip(text = badgeText, color = badgeColor)
    }
    VSpacer(4.dp)
}

@Composable
private fun MoneroBottomContent(
    uiState: ReceiveMoneroUiState,
    viewModel: ReceiveMoneroViewModel,
    onShowConfirmDialog: () -> Unit,
) {
    if (!uiState.watchAccount) {
        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            title = stringResource(R.string.receive_create_new_address),
            enabled = !uiState.isCreatingAddress,
            onClick = {
                if (uiState.addressBadge == AddressBadge.NEW && !viewModel.skipNewAddressConfirm) {
                    onShowConfirmDialog()
                } else {
                    viewModel.createNewAddress()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmNewAddressBottomSheet(
    onConfirm: (dontAskAgain: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var dontAskAgain by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ConfirmNewAddressSheetContent(
            dontAskAgain = dontAskAgain,
            onDontAskAgainChange = { dontAskAgain = it },
            onConfirm = { onConfirm(dontAskAgain) },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ConfirmNewAddressSheetContent(
    dontAskAgain: Boolean,
    onDontAskAgainChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_attention_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.receive_create_another_address_title),
        onCloseClick = onDismiss,
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.receive_create_another_address_warning),
        )

        VSpacer(12.dp)

        RowUniversal(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp)),
            onClick = { onDontAskAgainChange(!dontAskAgain) },
        ) {
            HsRadioButton(
                selected = dontAskAgain,
                onClick = { onDontAskAgainChange(!dontAskAgain) },
            )
            subhead2_leah(
                text = stringResource(R.string.receive_dont_ask_again),
            )
        }

        VSpacer(32.dp)

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.receive_create_new),
            onClick = onConfirm,
        )

        VSpacer(12.dp)

        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            title = stringResource(R.string.receive_keep_current),
            onClick = onDismiss,
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Suppress("UnusedPrivateMember")
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConfirmNewAddressSheetPreview() {
    ComposeAppTheme {
        ConfirmNewAddressSheetContent(
            dontAskAgain = false,
            onDontAskAgainChange = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
