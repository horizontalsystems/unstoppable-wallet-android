package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.BottomSheetTransactionOrderSelector
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.RbfSwitch
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsModule
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsViewModel
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.TransactionDataSortSettings
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.UtxoSwitch
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.InfoText
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Settings behind the unified send screen's icon for the Bitcoin family: transaction
 * sorting, coin control switch and replace-by-fee. All are persisted preferences; the fee
 * rate is adjusted on the confirmation step, where the send service estimates it.
 */
@Serializable
data class SendBtcSettingsPage(val wallet: Wallet) : HSPage() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val blockchainType = wallet.token.blockchainType
        val viewModel = viewModel<SendBtcAdvancedSettingsViewModel>(
            factory = SendBtcAdvancedSettingsModule.Factory(blockchainType)
        )
        val uiState = viewModel.uiState
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showSortSheet by remember { mutableStateOf(false) }

        HSScaffold(
            title = stringResource(R.string.Send_Advanced),
            onBack = navigation::removeLastOrNull,
            menuItems = listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_Reset),
                    onClick = { viewModel.reset() },
                    tint = ComposeAppTheme.colors.jacob
                )
            )
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (uiState.transactionSortingSupported) {
                    VSpacer(12.dp)
                    TransactionDataSortSettings(
                        navigation,
                        wallet.coin.code,
                        uiState.transactionSortTitle,
                    ) {
                        showSortSheet = true
                    }
                }

                VSpacer(32.dp)
                CellUniversalLawrenceSection(
                    listOf {
                        UtxoSwitch(
                            enabled = uiState.utxoExpertModeEnabled,
                            onChange = { viewModel.setUtxoExpertMode(it) }
                        )
                    }
                )
                InfoText(text = stringResource(R.string.Send_Utxo_Description))

                if (uiState.rbfVisible) {
                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            RbfSwitch(
                                enabled = uiState.rbfEnabled,
                                onChange = { viewModel.setRbfEnabled(it) }
                            )
                        }
                    )
                    InfoText(text = stringResource(R.string.Send_Rbf_Description))
                }

                VSpacer(32.dp)
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
        }
    }
}
