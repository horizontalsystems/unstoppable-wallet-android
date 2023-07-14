package cash.p.terminal.modules.withdrawcex.ui

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
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.withdrawcex.WithdrawCexViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.Badge
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_leah

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
                InfoText(text = stringResource(R.string.CexWithdraw_NetworkDescription))
                VSpacer(20.dp)
                CellUniversalLawrenceSection(mainViewModel.networks) { network ->
                    NetworkCell(
                        iconUrl = network.blockchain?.type?.imageUrl,
                        title = network.networkName,
                        selected = network.networkName == mainViewModel.uiState.networkName,
                        enabled = network.enabled
                    ) {
                        mainViewModel.onSelectNetwork(network)
                        onNavigateBack.invoke()
                    }
                }
                VSpacer(32.dp)
            }
        }
    }
}

@Composable
private fun NetworkCell(
    iconUrl: String?,
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onItemClick: () -> Unit,
) {
    RowUniversal(
        onClick = if (enabled) onItemClick else null,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        CoinImage(
            iconUrl = iconUrl,
            placeholder = R.drawable.ic_platform_placeholder_24,
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
        if (selected) {
            Icon(
                painter = painterResource(id = R.drawable.icon_20_check_1),
                contentDescription = null,
                tint = ComposeAppTheme.colors.jacob
            )
        }
        if (!enabled) {
            Badge(text = stringResource(R.string.Suspended))
        }
    }
}