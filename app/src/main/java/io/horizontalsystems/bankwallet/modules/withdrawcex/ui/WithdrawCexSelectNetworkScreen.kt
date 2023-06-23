package cash.p.terminal.modules.withdrawcex.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.modules.withdrawcex.WithdrawCexModule
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah

private val networks = listOf(
    WithdrawCexModule.NetworkViewItem(
        title = "Fantom",
        imageSource = ImageSource.Local(R.drawable.fantom_erc20),
        selected = false
    ),
    WithdrawCexModule.NetworkViewItem(
        title = "Gnosis",
        imageSource = ImageSource.Local(R.drawable.gnosis_erc20),
        selected = true
    )
)

@Composable
fun WithdrawCexSelectNetworkScreen(
    mainViewModel: WithdrawCexViewModel,
    onNavigateBack: () -> Unit,
) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.CexWithdraw_Network),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onNavigateBack
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                networks.let { viewItems ->
                    if (viewItems.isEmpty()) {
                        ListEmptyView(
                            text = stringResource(R.string.EmptyResults),
                            icon = R.drawable.ic_not_found
                        )
                    } else {
                        InfoText(text = stringResource(R.string.CexWithdraw_NetworkDescription))
                        VSpacer(20.dp)
                        CellUniversalLawrenceSection(viewItems) { viewItem ->
                            NetworkCell(
                                title = viewItem.title,
                                imageSource = viewItem.imageSource,
                                selected = viewItem.selected,
                                onItemClick = {
                                    onNavigateBack.invoke()
                                },
                            )
                        }
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkCell(
    title: String,
    imageSource: ImageSource,
    selected: Boolean,
    onItemClick: () -> Unit,
) {
    RowUniversal(
        onClick = onItemClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        Image(
            painter = imageSource.painter(),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                body_leah(
                    text = title,
                    maxLines = 1,
                )
            }
        }
        if(selected) {
            Icon(
                painter = painterResource(id = R.drawable.icon_20_check_1),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }
    }
}