package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendBitcoinAdapter
import io.horizontalsystems.walletkit.core.stringResId
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.v2.SendViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendSettingsCard
import io.horizontalsystems.walletkit.modules.send.v2.SendSettingsRow
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuGroup
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuItemX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule

/**
 * Settings behind the unified send screen's icon for the Bitcoin family: coin control,
 * transaction sorting, timelock and replace-by-fee. The fee rate is adjusted on the
 * confirmation step, where the send service estimates it.
 *
 * [address] is the recipient chosen so far; the timelock row is enabled only for a legacy
 * address, since the kit locks only P2PKH outputs.
 */
@Serializable
data class SendBtcSettingsPage(val wallet: Wallet, val address: String?) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val blockchainType = wallet.token.blockchainType
        val viewModel = viewModel<SendBtcAdvancedSettingsViewModel>(
            factory = SendBtcAdvancedSettingsModule.Factory(blockchainType)
        )
        val uiState = viewModel.uiState
        val sendViewModel = navigation.viewModelForScreen<SendViewModel>(SendV2Page::class, SendViewModel.Factory(wallet))
        val settings = sendViewModel.uiState.chainSettings as? BtcSendSettings ?: BtcSendSettings()
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) }

        // Counting the outputs queries the kit's storage, so it runs once, off the main thread.
        var total by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(adapter) {
            total = adapter?.let { withContext(Dispatchers.IO) { it.unspentOutputs.size } }
        }

        var showSortMenu by remember { mutableStateOf(false) }
        var showLockTimeMenu by remember { mutableStateOf(false) }

        val privateSend = sendViewModel.uiState.isPrivateSend
        val timeLockSupported = BtcSendSettings.timeLockSupported(blockchainType)
        // A private send deposit must stay immediately spendable, so no lock under that tab.
        val timeLockAvailable = BtcSendSettings.timeLockAvailable(blockchainType, address) && !privateSend
        val modified = uiState.transactionSortOptions.any { it.selected && it.mode != TransactionDataSortMode.Shuffle } ||
                !uiState.rbfEnabled ||
                settings.unspentOutputs != null ||
                settings.lockTimeInterval != null

        HSScaffold(
            title = stringResource(R.string.Send_Advanced),
            onBack = navigation::removeLastOrNull,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    enabled = modified,
                    tint = ComposeAppTheme.colors.jacob,
                    onClick = {
                        viewModel.reset()
                        sendViewModel.onChangeChainSettings(null)
                    },
                )
            )
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // The rows are gathered first so hairlines only appear between shown rows.
                val rows = mutableListOf<@Composable () -> Unit>()
                rows += {
                    SendSettingsRow(
                        title = stringResource(R.string.Send_Utxos),
                        subtitle = stringResource(R.string.Send_Utxos_Description),
                        // No selection means the wallet picks the outputs itself, which is not the same
                        // as having chosen all of them.
                        value = when {
                            total == null -> ""
                            settings.unspentOutputs == null -> stringResource(R.string.Send_Utxos_Auto)
                            else -> "${settings.unspentOutputs.size}/$total"
                        },
                        enabled = adapter != null && total != null,
                        onClick = { navigation.slideFromRight(SendBtcUtxoExpertModePage(wallet)) },
                    )
                }
                if (uiState.transactionSortingSupported) {
                    rows += {
                        SendSettingsRow(
                            title = stringResource(R.string.BtcBlockchainSettings_InputsOutputs),
                            subtitle = stringResource(R.string.Send_InputsOutputs_Description),
                            value = uiState.transactionSortTitle,
                            enabled = true,
                            onClick = { showSortMenu = true },
                        )
                    }
                }
                if (timeLockSupported) {
                    rows += {
                        val interval = settings.lockTimeInterval.takeIf { timeLockAvailable }
                        SendSettingsRow(
                            title = stringResource(R.string.Send_TimeLock),
                            subtitle = stringResource(R.string.Send_Hodler_Description),
                            warning = if (privateSend) stringResource(R.string.Send_Hodler_PrivateSendUnavailable) else null,
                            value = stringResource(interval.stringResId()),
                            enabled = timeLockAvailable,
                            onClick = { showLockTimeMenu = true },
                        )
                    }
                }
                if (uiState.rbfVisible) {
                    rows += {
                        CellPrimary(
                            middle = {
                                CellMiddleInfo(
                                    title = stringResource(R.string.Send_Rbf).hs,
                                    subtitle = stringResource(R.string.Send_Rbf_Description).hs,
                                )
                            },
                            right = {
                                CellRightControlsSwitcher(
                                    checked = uiState.rbfEnabled,
                                    onCheckedChange = { viewModel.setRbfEnabled(it) },
                                )
                            },
                        )
                    }
                }

                SendSettingsCard(rows)
            }

            if (showSortMenu) {
                MenuGroup(
                    title = stringResource(R.string.BtcBlockchainSettings_InputsOutputs),
                    items = uiState.transactionSortOptions.map {
                        MenuItemX(stringResource(it.mode.titleShort), it.selected, it.mode)
                    },
                    onDismissRequest = { showSortMenu = false },
                    onSelectItem = { viewModel.setTransactionMode(it) }
                )
            }

            if (showLockTimeMenu) {
                val intervals: List<LockTimeInterval?> = listOf(null) + LockTimeInterval.entries
                MenuGroup(
                    title = stringResource(R.string.Send_TimeLock),
                    items = intervals.map {
                        MenuItemX(stringResource(it.stringResId()), it == settings.lockTimeInterval, it)
                    },
                    onDismissRequest = { showLockTimeMenu = false },
                    onSelectItem = {
                        sendViewModel.onChangeChainSettings(settings.copy(lockTimeInterval = it))
                        showLockTimeMenu = false
                    }
                )
            }
        }
    }
}
