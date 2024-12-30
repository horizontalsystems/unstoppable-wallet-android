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

internal class StackingViewModel(
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    private val marketKitWrapper: MarketKitWrapper
) : ViewModel() {

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

    fun loadData() {
        createWalletIfNotExist()
        println("active wallet account active = ${accountManager.activeAccount != null}")
        println("active wallet count ${walletManager.activeWallets.size}")
        walletManager.activeWallets.forEach { wallet ->
            println("active wallet $wallet")
        }
    }

    private fun createWalletIfNotExist() {
        val contract = getContract()
        if (!isTokenExists(contract)) {
            val account = accountManager.activeAccount ?: return
            val tokenQuery = TokenQuery(BlockchainType.BinanceSmartChain, TokenType.Eip20(contract))
            marketKitWrapper.token(tokenQuery)?.let { token ->
                val wallet = Wallet(token, account)
                walletManager.save(listOf(wallet))
            }
        }
    }

    private fun isTokenExists(token: String): Boolean {
        return walletManager.activeWallets.any {
            it.token.type is TokenType.Eip20 && (it.token.type as TokenType.Eip20).address == token
        }
    }

    private fun getContract(): String =
        if (uiState.value.tabs.find { it.selected }?.item == StackingType.PCASH) {
            BuildConfig.PIRATE_CONTRACT
        } else {
            BuildConfig.COSANTA_CONTRACT
        }

    fun setStackingType(stackingType: StackingType) {
        _uiState.value = uiState.value.copy(
            tabs = uiState.value.tabs.map {
                it.copy(selected = it.item == stackingType)
            }
        )
    }
}