package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.v2.SendViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import kotlinx.serialization.Serializable
import io.horizontalsystems.walletkit.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen

/** Manual output selection for [SendBtcSettingsPage]; the choice is kept in the send view model. */
@Serializable
data class SendBtcUtxoExpertModePage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val sendViewModel = navigation.viewModelForScreen<SendViewModel>(SendV2Page::class, SendViewModel.Factory(wallet))
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) }

        // After process death the adapter may not exist yet; leave rather than crash.
        LaunchedEffect(adapter) {
            if (adapter == null) navigation.removeLastOrNull()
        }
        adapter ?: return

        val current = sendViewModel.uiState.chainSettings as? BtcSendSettings ?: BtcSendSettings()
        UtxoExpertModeScreen(
            adapter = adapter,
            token = wallet.token,
            customUnspentOutputs = current.unspentOutputs,
            updateUnspentOutputs = { outputs ->
                sendViewModel.onChangeChainSettings(current.copy(unspentOutputs = outputs.ifEmpty { null }))
            },
            onBackClick = { navigation.removeLastOrNull() },
        )
    }
}
