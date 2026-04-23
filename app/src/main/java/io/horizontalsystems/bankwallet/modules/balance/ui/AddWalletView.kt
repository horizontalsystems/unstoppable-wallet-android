package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.createaccount.WalletType
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun AddWalletView(
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    iconTint: Color = LocalContentColor.current,
    onNewWalletClick: () -> Unit,
    onWalletRestoreClick: () -> Unit,
    onWatchWalletClick: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let {
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .size(72.dp),
                painter = it,
                contentDescription = null,
                tint = iconTint
            )
            VSpacer(8.dp)
        }
        VSpacer(16.dp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section {
                WalletType(
                    icon = painterResource(R.drawable.ic_add_24),
                    title = stringResource(R.string.ManageAccounts_CreateNewWallet).hs,
                    subtitle = stringResource(R.string.ManageAccounts_CreateNewWalletDescription).hs,
                    borderTop = false,
                    onClick = onNewWalletClick
                )
                WalletType(
                    icon = painterResource(R.drawable.arrow_in_24),
                    title = stringResource(R.string.ManageAccounts_ExistingWallet).hs,
                    subtitle = stringResource(R.string.ManageAccounts_ExistingWalletDescription).hs,
                    borderTop = true,
                    onClick = onWalletRestoreClick
                )
                WalletType(
                    icon = painterResource(R.drawable.eye_on_24),
                    title = stringResource(R.string.ManageAccounts_ViewOnlyWallet).hs,
                    subtitle = stringResource(R.string.ManageAccounts_ViewOnlyWalletDescription).hs,
                    borderTop = true,
                    onClick = onWatchWalletClick
                )
            }
        }
    }
}

@Preview
@Composable
fun Preview_AddWalletView() {
    HSPreview {
        AddWalletView(
            icon = painterResource(R.drawable.ic_warning_filled_24),
            onNewWalletClick = { },
            onWalletRestoreClick = { },
            onWatchWalletClick = { },
        )
    }
}