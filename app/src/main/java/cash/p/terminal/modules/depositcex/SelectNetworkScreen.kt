package cash.p.terminal.modules.depositcex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.providers.CexDepositNetwork
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui.compose.components.Badge
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.imageUrl

@Composable
fun SelectNetworkScreen(
    networks: List<CexDepositNetwork>,
    onSelectNetwork: (CexDepositNetwork) -> Unit,
    onNavigateBack: (() -> Unit),
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Cex_ChooseNetwork),
                navigationIcon = { HsBackButton(onClick = onNavigateBack) },
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

@Composable
private fun NetworkCell(
    item: CexDepositNetwork,
    onItemClick: () -> Unit,
) {
    RowUniversal(
        onClick = if (item.enabled) onItemClick else null,
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalPadding = 0.dp
    ) {
        HsImage(
            url = item.blockchain?.type?.imageUrl,
            placeholder = R.drawable.ic_platform_placeholder_24,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(32.dp)
        )
        HSpacer(width = 16.dp)
        body_leah(
            modifier = Modifier.weight(1f),
            text = item.networkName,
            maxLines = 1,
        )
        HSpacer(width = 16.dp)
        if (item.enabled) {
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
