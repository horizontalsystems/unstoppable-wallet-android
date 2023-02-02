package io.horizontalsystems.bankwallet.ui.extensions

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun WalletSwitchBottomSheet(
    wallets: List<Account>,
    watchingAddresses: List<Account>,
    selectedAccount: Account?,
    onSelectListener: (Account) -> Unit,
    onCancelClick: () -> Unit
) {
    val comparator = compareBy<Account> { it.name.lowercase() }

    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.icon_24_lock),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        title = stringResource(R.string.ManageAccount_SwitchWallet_Title),
        onCloseClick = onCancelClick,
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
