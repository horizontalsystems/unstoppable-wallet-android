package io.horizontalsystems.walletkit.modules.send.monero.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.v2.SendSettingsCard
import io.horizontalsystems.walletkit.modules.send.v2.SendSettingsRow
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import io.horizontalsystems.walletkit.modules.send.v2.SendViewModel
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import kotlinx.serialization.Serializable
import io.horizontalsystems.walletkit.modules.send.monero.utxoexpert.MoneroUtxoExpertModeScreen

/**
 * Settings behind the unified send screen's icon for Monero: coin control only. The wallet
 * does not reveal which inputs its own selection will use before the transaction is built,
 * so the row counts every spendable output until the user picks manually.
 */
@Serializable
data class SendMoneroSettingsPage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val sendViewModel = navigation.viewModelForScreen<SendViewModel>(SendV2Page::class, SendViewModel.Factory(wallet))
        val settings = sendViewModel.uiState.chainSettings as? MoneroSendSettings ?: MoneroSendSettings()
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendMoneroAdapter>(wallet) }

        // Reading the outputs takes the wallet lock, so it runs off the main thread. While the
        // wallet scans, outputs appear and disappear; a selection that references an output
        // no longer spendable is trimmed here so the balance and the count stay truthful.
        var total by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(adapter) {
            val outputs = adapter?.getUnspentOutputs() ?: return@LaunchedEffect
            total = outputs.size

            val selection = (sendViewModel.uiState.chainSettings as? MoneroSendSettings)?.unspentOutputs
                ?: return@LaunchedEffect
            val spendable = outputs.map { it.keyImage }.toSet()
            val pruned = selection.filter { it.keyImage in spendable }
            if (pruned.size != selection.size) {
                sendViewModel.onChangeChainSettings(MoneroSendSettings(pruned.ifEmpty { null }))
            }
        }

        val modified = settings.unspentOutputs != null

        HSScaffold(
            title = stringResource(R.string.Send_Advanced),
            onBack = navigation::removeLastOrNull,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = modified,
                    tint = ComposeAppTheme.colors.jacob,
                    onClick = { sendViewModel.onChangeChainSettings(null) },
                )
            )
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val used = settings.unspentOutputs?.size ?: total
                SendSettingsCard(
                    listOf {
                        SendSettingsRow(
                            title = stringResource(R.string.Send_Utxos),
                            subtitle = stringResource(R.string.Send_Utxos_Description),
                            value = if (total != null) "$used/$total" else "",
                            enabled = adapter != null && total != null,
                            onClick = { navigation.slideFromRight(SendMoneroUtxoExpertModePage(wallet)) },
                        )
                    }
                )
            }
        }
    }
}

/** Manual output selection for [SendMoneroSettingsPage]; the choice is kept in the send view model. */
@Serializable
data class SendMoneroUtxoExpertModePage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val sendViewModel = navigation.viewModelForScreen<SendViewModel>(SendV2Page::class, SendViewModel.Factory(wallet))
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendMoneroAdapter>(wallet) }

        // After process death the adapter may not exist yet; leave rather than crash.
        LaunchedEffect(adapter) {
            if (adapter == null) navigation.removeLastOrNull()
        }
        adapter ?: return

        val current = sendViewModel.uiState.chainSettings as? MoneroSendSettings ?: MoneroSendSettings()
        MoneroUtxoExpertModeScreen(
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
