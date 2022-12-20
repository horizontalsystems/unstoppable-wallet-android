package io.horizontalsystems.bankwallet.modules.settings.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsModule
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsModule
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun BlockchainSettingsBlock(
    blockchainSettingsViewModel: BlockchainSettingsViewModel,
    navController: NavController
) {
    CellUniversalLawrenceSection(blockchainSettingsViewModel.viewItems) { item ->
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
                is BlockchainSettingsModule.BlockchainItem.Solana -> {
                    navController.slideFromRight(R.id.solanaNetworkFragment, bundleOf())
                }
            }
        }
    }
    InfoText(
        text = stringResource(R.string.SecurityCenter_BlockchainSettingsFooterDescription),
    )
}

@Composable
private fun BlockchainSettingCell(
    item: BlockchainSettingsModule.BlockchainViewItem,
    onClick: () -> Unit
) {
    RowUniversal(
        onClick = onClick
    ) {
        Image(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp),
            painter = rememberAsyncImagePainter(
                model = item.imageUrl,
                error = painterResource(R.drawable.ic_platform_placeholder_32)
            ),
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            body_leah(text = item.title)
            subhead2_grey(text = item.subtitle)
        }
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
