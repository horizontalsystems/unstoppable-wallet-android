package cash.p.terminal.modules.depositcex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.providers.CexNetwork
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

@Composable
fun SelectNetworkScreen(
    networks: List<CexNetwork>,
    onSelectNetwork: (CexNetwork) -> Unit,
    onNavigateBack: (() -> Unit)?,
    onClose: () -> Unit,
) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                val navigationIcon: @Composable (() -> Unit)? = onNavigateBack?.let {
                    {
                        HsBackButton(onClick = onNavigateBack)
                    }
                }

                AppBar(
                    title = TranslatableString.ResString(R.string.Cex_ChooseNetwork),
                    navigationIcon = navigationIcon,
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = onClose
                        )
                    )
                )
            }
        ) {
            Column(modifier = Modifier.padding(it)) {
                InfoText(text = stringResource(R.string.Cex_ChooseNetwork_Description))
                VSpacer(20.dp)
                CellUniversalLawrenceSection(networks) { cexNetwork ->
                    NetworkCell(
                        item = cexNetwork,
                        onItemClick = {
                            onSelectNetwork.invoke(cexNetwork)
                        },
                    )
                }
                VSpacer(32.dp)
            }
        }
    }
}

@Composable
private fun NetworkCell(
    item: CexNetwork,
    onItemClick: () -> Unit,
) {
    RowUniversal(
        onClick = if (item.depositEnabled) onItemClick else null,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        CoinImage(
            iconUrl = null,
            placeholder = R.drawable.ic_platform_placeholder_24,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(32.dp)
        )
        HSpacer(width = 16.dp)
        body_leah(
            modifier = Modifier.weight(1f),
            text = item.name,
            maxLines = 1,
        )
        HSpacer(width = 16.dp)
        if (item.depositEnabled) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        } else {
            Badge(text = stringResource(R.string.Suspended))
        }
    }
}
