package cash.p.terminal.ui.extensions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsRadioButton
import cash.p.terminal.ui_compose.components.RowUniversal
import kotlinx.coroutines.launch

/**
 * Material3 ModalBottomSheet for wallet selection.
 * Self-contained - manages its own sheet state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSwitchBottomSheet(
    wallets: List<Account>,
    watchingAddresses: List<Account>,
    selectedAccount: Account?,
    onSelectListener: (Account) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.ManageAccount_SwitchWallet_Title)
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        WalletSwitchContent(
            wallets = wallets,
            watchingAddresses = watchingAddresses,
            selectedAccount = selectedAccount,
            onSelectListener = { account ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onSelectListener(account)
                    onDismiss()
                }
            },
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            },
            title = title
        )
    }
}

@Composable
private fun WalletSwitchContent(
    wallets: List<Account>,
    watchingAddresses: List<Account>,
    selectedAccount: Account?,
    onSelectListener: (Account) -> Unit,
    onCloseClick: () -> Unit,
    title: String
) {
    val comparator = compareBy<Account> { it.name.lowercase() }

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.icon_24_lock),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = title,
        onCloseClick = onCloseClick,
    ) {

        Spacer(Modifier.height(12.dp))

        if (wallets.isNotEmpty()) {
            HeaderText(
                text = stringResource(R.string.ManageAccount_Wallets)
            )
            Section(
                items = wallets.sortedWith(comparator),
                selectedItem = selectedAccount,
                onSelectListener = onSelectListener,
            )
        }

        if (watchingAddresses.isNotEmpty()) {
            if (wallets.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
            }
            HeaderText(
                text = stringResource(R.string.ManageAccount_WatchAddresses)
            )
            Section(
                items = watchingAddresses.sortedWith(comparator),
                selectedItem = selectedAccount,
                onSelectListener = onSelectListener,
            )
        }

        Spacer(Modifier.height(44.dp))
    }
}

@Composable
private fun Section(
    items: List<Account>,
    selectedItem: Account?,
    onSelectListener: (Account) -> Unit,
) {
    CellUniversalLawrenceSection(items, showFrame = true) { item ->
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                onSelectListener.invoke(item)
            },
        ) {
            HsRadioButton(
                selected = item == selectedItem,
                onClick = {
                    onSelectListener.invoke(item)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                body_leah(text = item.name)
                subhead2_grey(text = item.type.detailedDescription)
            }
            if (item.isWatchAccount) {
                Icon(
                    modifier = Modifier.padding(start = 16.dp),
                    painter = painterResource(id = R.drawable.ic_eye_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Preview
@Composable
private fun WalletSwitchContentPreview() {
    val wallets = listOf(
        Account(
            id = "1",
            name = "Wallet 1",
            type = AccountType.Mnemonic(words = listOf(), passphrase = ""),
            origin = AccountOrigin.Created,
            level = 0
        ),
        Account(
            id = "2",
            name = "Wallet 2",
            type = AccountType.Mnemonic(words = listOf(), passphrase = ""),
            origin = AccountOrigin.Restored,
            level = 0
        )
    )
    val watchingAddresses = listOf(
        Account(
            id = "3",
            name = "Watch Address",
            type = AccountType.EvmAddress(address = "0x1234567890"),
            origin = AccountOrigin.Restored,
            level = 0
        )
    )

    ComposeAppTheme {
        WalletSwitchContent(
            wallets = wallets,
            watchingAddresses = watchingAddresses,
            selectedAccount = wallets.first(),
            onSelectListener = {},
            onCloseClick = {},
            title = "Switch Wallet"
        )
    }
}
