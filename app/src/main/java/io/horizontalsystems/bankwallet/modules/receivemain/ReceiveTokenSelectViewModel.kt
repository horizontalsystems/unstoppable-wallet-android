package io.horizontalsystems.bankwallet.modules.receivemain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceActiveWalletRepository
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect

class ReceiveTokenSelectViewModel(
    private val activeWalletRepository: BalanceActiveWalletRepository
) : ViewModel() {
    private var query: String? = null
    private var wallets: List<Wallet> = listOf()
    private var coins: List<Coin> = listOf()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            coins = coins
        )
    )

    init {
        viewModelScope.launch {
            activeWalletRepository.itemsObservable.collect {
                wallets = it.sortedBy { it.coin.code }
                refreshItems()

                emitState()
            }
        }
    }

    fun updateFilter(q: String) {
        viewModelScope.launch {
            query = q
            refreshItems()

            emitState()
        }
    }

    private fun refreshItems() {
        var result = wallets
            .map { it.coin }
            .distinct()

        query?.let { tmpQuery ->
            result = result.filter { coin ->
                coin.code.contains(tmpQuery, true) || coin.name.contains(tmpQuery, true)
            }
        }

        coins = result
    }


    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                coins = coins,
            )
        }
    }

    fun getCoinActiveWalletsType(coin: Coin): CoinActiveWalletsType? {
        val coinWallets = wallets.filter { it.coin == coin }
        val singleWallet = coinWallets.singleOrNull()

        return when {
            singleWallet != null -> {
                CoinActiveWalletsType.Single(singleWallet)
            }
            coinWallets.all { it.token.type is TokenType.Derived } -> {
                CoinActiveWalletsType.MultipleDerivations
            }
            coinWallets.all { it.token.type is TokenType.AddressTyped } -> {
                CoinActiveWalletsType.MultipleAddressTypes
            }
            coinWallets.isNotEmpty() -> {
                CoinActiveWalletsType.MultipleBlockchains
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

sealed interface CoinActiveWalletsType {
    data class Single(val wallet: Wallet) : CoinActiveWalletsType
    object MultipleDerivations : CoinActiveWalletsType
    object MultipleAddressTypes : CoinActiveWalletsType
    object MultipleBlockchains : CoinActiveWalletsType
}

data class ReceiveTokenSelectUiState(
    val coins: List<Coin>
)
