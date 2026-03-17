package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsScreen
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkScreen
import io.horizontalsystems.bankwallet.modules.moneronetwork.MoneroNetworkScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.solananetwork.SolanaNetworkScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable

@Serializable
data object BlockchainSettingsScreen : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        BlockchainSettingsScreen(
            backStack = backStack,
        )
    }
}

class BlockchainSettingsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
//        BlockchainSettingsScreen(
//            backStack = navController,
//        )
    }

}

@Composable
private fun BlockchainSettingsScreen(
    backStack: NavBackStack<HSScreen>,
    viewModel: BlockchainSettingsViewModel = viewModel(factory = BlockchainSettingsModule.Factory()),
) {

    HSScaffold(
        title = stringResource(R.string.BlockchainSettings_Title),
        onBack = backStack::removeLastOrNull,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            VSpacer(12.dp)
            BlockchainSettingsBlock(
                btcLikeChains = viewModel.btcLikeChains,
                otherChains = viewModel.otherChains,
                backStack = backStack
            )
            VSpacer(44.dp)
        }
    }
}

@Composable
fun BlockchainSettingsBlock(
    btcLikeChains: List<BlockchainSettingsModule.BlockchainViewItem>,
    otherChains: List<BlockchainSettingsModule.BlockchainViewItem>,
    backStack: NavBackStack<HSScreen>
) {
    CellUniversalLawrenceSection(btcLikeChains) { item ->
        BlockchainSettingCell(item) {
            onClick(item, backStack)
        }
    }
    Spacer(Modifier.height(32.dp))
    CellUniversalLawrenceSection(otherChains) { item ->
        BlockchainSettingCell(item) {
            onClick(item, backStack)
        }
    }
}

private fun onClick(
    item: BlockchainSettingsModule.BlockchainViewItem,
    backStack: NavBackStack<HSScreen>
) {
    when (item.blockchainItem) {
        is BlockchainSettingsModule.BlockchainItem.Btc -> {
            backStack.add(BtcBlockchainSettingsScreen(item.blockchainItem.blockchain))

            stat(
                page = StatPage.BlockchainSettings,
                event = StatEvent.OpenBlockchainSettingsBtc(item.blockchainItem.blockchain.uid)
            )
        }

        is BlockchainSettingsModule.BlockchainItem.Evm -> {
            backStack.add(EvmNetworkScreen(item.blockchainItem.blockchain))

            stat(
                page = StatPage.BlockchainSettings,
                event = StatEvent.OpenBlockchainSettingsEvm(item.blockchainItem.blockchain.uid)
            )
        }

        is BlockchainSettingsModule.BlockchainItem.Solana -> {
            backStack.add(SolanaNetworkScreen)

            stat(
                page = StatPage.BlockchainSettings,
                event = StatEvent.Open(StatPage.BlockchainSettingsSolana)
            )
        }

        is BlockchainSettingsModule.BlockchainItem.Monero -> {
            backStack.add(MoneroNetworkScreen)

            stat(
                page = StatPage.BlockchainSettings,
                event = StatEvent.OpenBlockchainSettingsEvm(item.blockchainItem.blockchain.uid)
            )
        }
    }
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
