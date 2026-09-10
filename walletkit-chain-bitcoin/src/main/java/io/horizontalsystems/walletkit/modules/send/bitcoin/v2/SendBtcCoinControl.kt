package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoCell
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoData
import io.horizontalsystems.walletkit.modules.send.bitcoin.UtxoType
import io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import io.horizontalsystems.walletkit.modules.send.v2.SendChainSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import kotlinx.serialization.Serializable

/**
 * Coin control row on the unified send screen, shown while the expert mode preference is
 * on. Selected outputs go into [settings] and reach the confirmation's send service; the
 * count of outputs an automatic selection would spend is only known once the fee is
 * estimated there, so the row shows the wallet's total until the user picks manually.
 */
@Composable
fun SendBtcScreenExtras(
    navigation: HSNavigation,
    wallet: Wallet,
    settings: SendChainSettingsViewModel,
) {
    val expertModeEnabled by App.localStorage.utxoExpertModeEnabledFlow.collectAsState()
    if (!expertModeEnabled) return

    val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) }
        ?: return
    val selected = (settings.settings as? BtcSendSettings)?.unspentOutputs
    val total = adapter.unspentOutputs.size
    val utxoData = if (selected == null) {
        UtxoData(type = UtxoType.Auto, value = "$total / $total")
    } else {
        UtxoData(type = UtxoType.Manual, value = "${selected.size} / $total")
    }

    VSpacer(16.dp)
    UtxoCell(utxoData = utxoData) {
        navigation.slideFromRight(SendBtcUtxoExpertModePage(wallet))
    }
}

/** Manual output selection for [SendBtcScreenExtras]. */
@Serializable
data class SendBtcUtxoExpertModePage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val settings = navigation.viewModelForScreen<SendChainSettingsViewModel>(SendV2Page::class)
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) }

        // After process death the adapter may not exist yet; leave rather than crash.
        LaunchedEffect(adapter) {
            if (adapter == null) navigation.removeLastOrNull()
        }
        adapter ?: return

        UtxoExpertModeScreen(
            adapter = adapter,
            token = wallet.token,
            customUnspentOutputs = (settings.settings as? BtcSendSettings)?.unspentOutputs,
            updateUnspentOutputs = { outputs ->
                settings.settings = BtcSendSettings(outputs.ifEmpty { null })
            },
            onBackClick = { navigation.removeLastOrNull() },
        )
    }
}
