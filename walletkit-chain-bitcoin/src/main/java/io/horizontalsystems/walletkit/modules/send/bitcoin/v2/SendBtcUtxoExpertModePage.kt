package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import io.horizontalsystems.walletkit.modules.send.v2.SendChainSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import kotlinx.serialization.Serializable

/** Manual output selection for [SendBtcSettingsPage]; the choice is kept in the send page's settings. */
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

        val current = settings.settings as? BtcSendSettings ?: BtcSendSettings()
        UtxoExpertModeScreen(
            adapter = adapter,
            token = wallet.token,
            customUnspentOutputs = current.unspentOutputs,
            updateUnspentOutputs = { outputs ->
                settings.settings = current.copy(unspentOutputs = outputs.ifEmpty { null })
            },
            onBackClick = { navigation.removeLastOrNull() },
        )
    }
}
