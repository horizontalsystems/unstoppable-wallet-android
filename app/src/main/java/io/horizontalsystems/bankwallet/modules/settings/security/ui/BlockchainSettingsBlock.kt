package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsModule
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellMultilineLawrenceSection

@Composable
fun BlockchainSettingsBlock(
    blockchainSettingsViewModel: BlockchainSettingsViewModel,
    navController: NavController
) {
    CellMultilineLawrenceSection(blockchainSettingsViewModel.viewItems) { item ->
        BlockchainSettingCell(item) {
            when(item.blockchainItem){
                is BlockchainSettingsModule.BlockchainItem.Btc -> {
                    val params = BtcBlockchainSettingsModule.args(item.blockchainItem.blockchain)
                    navController.slideFromRight(R.id.btcBlockchainSettingsFragment, params)
                }
                is BlockchainSettingsModule.BlockchainItem.Evm -> {
                    val params = EvmNetworkModule.args(item.blockchainItem.blockchain)
                    navController.slideFromRight(R.id.evmNetworkFragment, params)
                }
            }
        }
    }
    Text(
        text = stringResource(R.string.SecurityCenter_BlockchainSettingsFooterDescription),
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun BlockchainSettingCell(
    item: BlockchainSettingsModule.BlockchainViewItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(item.icon),
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
            )
            Text(
                text = item.subtitle,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
            )
        }

        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
