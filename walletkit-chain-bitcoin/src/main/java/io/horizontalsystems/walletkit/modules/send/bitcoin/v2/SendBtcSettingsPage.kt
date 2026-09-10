package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.BottomSheetTransactionOrderSelector
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendChainSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.v2.SendV2Page
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.CellGroup
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightSelectors
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuGroup
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuItemX
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val blockchainType = wallet.token.blockchainType
        val viewModel = viewModel<SendBtcAdvancedSettingsViewModel>(
            factory = SendBtcAdvancedSettingsModule.Factory(blockchainType)
        )
        val uiState = viewModel.uiState
        val chainSettings = navigation.viewModelForScreen<SendChainSettingsViewModel>(SendV2Page::class)
        val settings = chainSettings.settings as? BtcSendSettings ?: BtcSendSettings()
        val adapter = remember(wallet) { App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) }

        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showSortSheet by remember { mutableStateOf(false) }
        var showLockTimeMenu by remember { mutableStateOf(false) }

        val timeLockSupported = BtcSendSettings.timeLockSupported(blockchainType)
        val timeLockAvailable = BtcSendSettings.timeLockAvailable(blockchainType, address)
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
                        chainSettings.settings = null
                    },
                )
            )
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CellGroup(paddingValues = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp)) {
                    val total = adapter?.unspentOutputs?.size ?: 0
                    val used = settings.unspentOutputs?.size ?: total
                    SettingsRow(
                        title = stringResource(R.string.Send_Utxos),
                        subtitle = stringResource(R.string.Send_Utxos_Description),
                        value = "$used / $total",
                        enabled = adapter != null,
                        onClick = { navigation.slideFromRight(SendBtcUtxoExpertModePage(wallet)) },
                    )

                    if (uiState.transactionSortingSupported) {
                        SettingsRow(
                            title = stringResource(R.string.BtcBlockchainSettings_InputsOutputs),
                            subtitle = stringResource(R.string.Send_InputsOutputs_Description),
                            value = uiState.transactionSortTitle,
                            enabled = true,
                            onClick = { showSortSheet = true },
                        )
                    }

                    if (timeLockSupported) {
                        val interval = settings.lockTimeInterval.takeIf { timeLockAvailable }
                        SettingsRow(
                            title = stringResource(R.string.Send_TimeLock),
                            subtitle = stringResource(R.string.Send_Hodler_Description),
                            value = stringResource(interval.stringResId()),
                            enabled = timeLockAvailable,
                            onClick = { showLockTimeMenu = true },
                        )
                    }

                    if (uiState.rbfVisible) {
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
            }

            if (showSortSheet) {
                BottomSheetContent(
                    onDismissRequest = { showSortSheet = false },
                    sheetState = sheetState
                ) {
                    BottomSheetTransactionOrderSelector(
                        items = uiState.transactionSortOptions,
                        onSelect = { viewModel.setTransactionMode(it) },
                        onCloseClick = {
                            scope.launch {
                                sheetState.hide()
                                showSortSheet = false
                            }
                        }
                    )
                }
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
                        chainSettings.settings = settings.copy(lockTimeInterval = it)
                        showLockTimeMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val valueColor = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey
    CellPrimary(
        middle = {
            CellMiddleInfo(
                title = title.hs,
                subtitle = subtitle.hs,
            )
        },
        right = {
            CellRightSelectors(
                subtitle = value.hs(color = valueColor),
                icon = painterResource(R.drawable.arrow_s_down_20),
                iconTint = valueColor,
            )
        },
        onClick = if (enabled) onClick else null,
    )
}
