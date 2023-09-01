package cash.p.terminal.modules.receivemain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.balance.BalanceActiveWalletRepository
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect

class ReceiveTokenSelectViewModel(
    private val activeWalletRepository: BalanceActiveWalletRepository
) : ViewModel() {
    private var wallets: List<Wallet> = listOf()
    private val coins: List<Coin>
        get() = wallets.map { it.coin }.distinct()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            coins = coins
        )
    )

    init {
        viewModelScope.launch {
            activeWalletRepository.itemsObservable.collect {
                wallets = it.sortedBy { it.coin.code }

                emitState()
            }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                coins = coins,
            )
        }
    }

    fun getXxx(coin: Coin): ReceiveAddressXxxState? {
        val coinWallets = wallets.filter { it.coin == coin }
        val singleWallet = coinWallets.singleOrNull()

        return when {
            singleWallet != null -> {
                ReceiveAddressXxxState.Simple(singleWallet)
            }
            coinWallets.all { it.token.type is TokenType.Derived } -> {
                ReceiveAddressXxxState.ChooseDerivationType
            }
            coinWallets.all { it.token.type is TokenType.AddressTyped } -> {
                ReceiveAddressXxxState.ChooseAddressType
            }
            coinWallets.isNotEmpty() -> {
                ReceiveAddressXxxState.ChooseNetwork
            }
            else -> {
                null
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveTokenSelectViewModel(
                BalanceActiveWalletRepository(App.walletManager, App.evmSyncSourceManager)
            ) as T
        }
    }
}

sealed interface ReceiveAddressXxxState {
    object ChooseNetwork : ReceiveAddressXxxState
    object ChooseDerivationType : ReceiveAddressXxxState
    object ChooseAddressType : ReceiveAddressXxxState
    data class Simple(val wallet: Wallet) : ReceiveAddressXxxState
}

data class ReceiveTokenSelectUiState(
    val coins: List<Coin>
)
