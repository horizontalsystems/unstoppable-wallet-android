package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock

@Composable
fun AddressFormatSelectScreen(
    addressFormatItems: List<AddressFormatItem>,
    description: String,
    onSelect: (Wallet) -> Unit,
    closeModule: () -> Unit,
    onBackPress: () -> Unit
) {
    HSScaffold(
        title = stringResource(R.string.Balance_Receive_AddressFormat),
        onBack = onBackPress,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = closeModule
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.tyler)
                    .fillMaxWidth()
            ) {
                TextBlock(
                    stringResource(R.string.Balance_Receive_AddressFormatDescription)
                )
                VSpacer(20.dp)
            }

            addressFormatItems.forEach { item ->
                AddressFormatCell(
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = {
                        onSelect.invoke(item.wallet)
                    }
                )
                HsDivider()
            }
            VSpacer(32.dp)
            AlertCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                format = AlertFormat.Structured,
                type = AlertType.Caution,
                text = description,
            )
        }
    }
}

@Composable
fun AddressFormatCell(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = title.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightNavigation()
        },
        onClick = onClick
    )
}

data class AddressFormatItem(val title: String, val subtitle: String, val wallet: Wallet)