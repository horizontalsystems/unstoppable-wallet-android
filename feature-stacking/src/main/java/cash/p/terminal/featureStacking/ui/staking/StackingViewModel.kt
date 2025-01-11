package cash.p.terminal.featureStacking.ui.staking

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import cash.p.terminal.featureStacking.BuildConfig
import cash.p.terminal.featureStacking.R
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui_compose.components.TabItem
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType

internal class StackingViewModel : ViewModel() {

    private val _uiState =
        mutableStateOf(
            StackingUIState(
                tabs = listOf(
                    TabItem(
                        title = Translator.getString(R.string.pirate_cash),
                        selected = true,
                        item = StackingType.PCASH
                    ),
                    TabItem(
                        title = Translator.getString(R.string.cosanta),
                        selected = false,
                        item = StackingType.COSANTA
                    )
                )
            )
        )
    val uiState: State<StackingUIState> get() = _uiState

    fun setStackingType(stackingType: StackingType) {
        _uiState.value = uiState.value.copy(
            tabs = uiState.value.tabs.map {
                it.copy(selected = it.item == stackingType)
            }
        )
    }
}