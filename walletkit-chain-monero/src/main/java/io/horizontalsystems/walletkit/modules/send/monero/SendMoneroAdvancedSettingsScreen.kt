package io.horizontalsystems.walletkit.modules.send.monero

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.modules.send.bitcoin.advanced.UtxoSwitch
import io.horizontalsystems.walletkit.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.walletkit.ui.compose.components.InfoText
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold

class SendMoneroAdvancedSettingsViewModel(
    private val localStorage: ILocalStorage,
) : ViewModelUiState<SendMoneroAdvancedSettingsViewModel.UiState>() {

    private var utxoExpertModeEnabled = localStorage.utxoExpertModeEnabled

    override fun createState() = UiState(
        utxoExpertModeEnabled = utxoExpertModeEnabled,
    )

    fun setUtxoExpertMode(enabled: Boolean) {
        utxoExpertModeEnabled = enabled
        localStorage.utxoExpertModeEnabled = enabled
        emitState()
    }

    data class UiState(
        val utxoExpertModeEnabled: Boolean,
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendMoneroAdvancedSettingsViewModel(App.localStorage) as T
        }
    }
}

@Composable
fun SendMoneroAdvancedSettingsScreen(onBack: () -> Unit) {
    val viewModel: SendMoneroAdvancedSettingsViewModel = viewModel(
        factory = SendMoneroAdvancedSettingsViewModel.Factory()
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Send_Advanced),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            VSpacer(12.dp)
            CellUniversalLawrenceSection(
                listOf {
                    UtxoSwitch(
                        enabled = uiState.utxoExpertModeEnabled,
                        onChange = { viewModel.setUtxoExpertMode(it) }
                    )
                }
            )
            InfoText(
                text = stringResource(R.string.Send_Utxo_Description),
            )
        }
    }
}
