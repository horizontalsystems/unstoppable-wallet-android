package cash.p.terminal.modules.receive.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.AppBar
import io.horizontalsystems.core.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import io.horizontalsystems.core.RowUniversal
import io.horizontalsystems.core.SectionUniversalItem
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun AddressFormatSelectScreen(
    addressFormatItems: List<AddressFormatItem>,
    description: String,
    onSelect: (cash.p.terminal.wallet.Wallet) -> Unit,
    onBackPress: () -> Unit
) {
    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Balance_Receive_AddressFormat),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
                menuItems = listOf()
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            InfoText(
                text = stringResource(R.string.Balance_Receive_AddressFormatDescription)
            )
            VSpacer(20.dp)
            CellUniversalLawrenceSection(addressFormatItems) { item ->
                SectionUniversalItem {
                    AddressFormatCell(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = {
                            onSelect.invoke(item.wallet)
                        }
                    )
                }
            }
            VSpacer(32.dp)
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = description
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
    RowUniversal(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            body_leah(text = title)
            subhead2_grey(text = subtitle)
        }
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}

data class AddressFormatItem(val title: String, val subtitle: String, val wallet: cash.p.terminal.wallet.Wallet)