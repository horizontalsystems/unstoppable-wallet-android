package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.core.eligibleTokens
import io.horizontalsystems.bankwallet.core.utils.Utils
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token

@HiltViewModel(assistedFactory = NetworkSelectViewModel.Factory::class)
class NetworkSelectViewModel @AssistedInject constructor(
    @Assisted val activeAccount: Account,
    @Assisted val fullCoin: FullCoin,
    private val walletManager: WalletManager,
    private val adapterManager: IAdapterManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(activeAccount: Account, fullCoin: FullCoin): NetworkSelectViewModel
    }

    val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

    suspend fun getOrCreateWallet(token: Token): Wallet {
        return walletManager
            .activeWallets
            .find { it.token == token }
            ?: createWallet(token)
    }

    private suspend fun createWallet(token: Token): Wallet {
        val wallet = Wallet(token, activeAccount)

        walletManager.save(listOf(wallet))

        Utils.waitUntil(1000L, 100L) {
            adapterManager.getReceiveAdapterForWallet(wallet) != null
        }

        return wallet
    }
}
