package cash.p.terminal.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.eligibleTokens
import cash.p.terminal.core.utils.Utils
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.FullCoin
import cash.p.terminal.wallet.useCases.GetHardwarePublicKeyForWalletUseCase
import org.koin.java.KoinJavaComponent.inject

class NetworkSelectViewModel(
    val activeAccount: Account,
    val fullCoin: FullCoin,
    private val walletManager: IWalletManager
) : ViewModel() {
    val eligibleTokens = fullCoin.eligibleTokens(activeAccount.type)

    private val getHardwarePublicKeyForWalletUseCase: GetHardwarePublicKeyForWalletUseCase by inject(
        GetHardwarePublicKeyForWalletUseCase::class.java
    )

    suspend fun getOrCreateWallet(token: Token): Wallet {
        return walletManager
            .activeWallets
            .find { it.token == token }
            ?: createWallet(token)
    }

    private suspend fun createWallet(token: Token): Wallet {

        val wallet = Wallet(
            token = token,
            account = activeAccount,
            hardwarePublicKey = getHardwarePublicKeyForWalletUseCase(activeAccount, token)
        )

        walletManager.save(listOf(wallet))

        Utils.waitUntil(1000L, 100L) {
            App.adapterManager.getReceiveAdapterForWallet(wallet) != null
        }

        return wallet
    }

    class Factory(
        private val activeAccount: Account,
        private val fullCoin: FullCoin
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NetworkSelectViewModel(activeAccount, fullCoin, App.walletManager) as T
        }
    }
}
