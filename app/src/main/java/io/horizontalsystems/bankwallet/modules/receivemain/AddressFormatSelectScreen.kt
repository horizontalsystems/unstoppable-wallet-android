package cash.p.terminal.modules.receivemain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
>>>>>>>> 5476c52de (Implement functionality for selecting BTC derivation type):app/src/main/java/cash.p.terminal/modules/receivemain/AddressFormatSelectScreen.kt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.receive.address.ReceiveAddressFragment
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.SectionUniversalItem
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah
import cash.p.terminal.ui.compose.components.subhead2_grey

@Composable
fun AddressFormatSelectScreen(
    navController: NavController,
    addressFormatItems: List<AddressFormatItem>,
    description: String,
>>>>>>>> 5476c52de (Implement functionality for selecting BTC derivation type):app/src/main/java/cash.p.terminal/modules/receivemain/AddressFormatSelectScreen.kt
) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Balance_Receive_AddressFormat),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
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
                                navController.slideFromBottom(
                                    R.id.receiveFragment,
                                    bundleOf(ReceiveAddressFragment.WALLET_KEY to item.wallet)
                                )
                            }
                        )
                    }
>>>>>>>> 5476c52de (Implement functionality for selecting BTC derivation type):app/src/main/java/cash.p.terminal/modules/receivemain/AddressFormatSelectScreen.kt
                }
                VSpacer(32.dp)
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = description
                )
            }
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

data class AddressFormatItem(val title: String, val subtitle: String, val wallet: Wallet)
>>>>>>>> 5476c52de (Implement functionality for selecting BTC derivation type):app/src/main/java/cash.p.terminal/modules/receivemain/AddressFormatSelectScreen.kt
