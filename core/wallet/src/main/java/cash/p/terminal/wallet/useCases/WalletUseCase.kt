package cash.p.terminal.wallet.useCases

import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet

class WalletUseCase(
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    private val adapterManager: IAdapterManager
) {
    fun getWallet(token: Token): Wallet? = walletManager.activeWallets.find {
        it.token == token
    }

    fun getWallet(coinUid: String, blockchainType: String): Wallet? =
        walletManager.activeWallets.find {
            it.token.coin.uid == coinUid && it.token.blockchainType.uid == blockchainType
        }

    fun createWallet(token: Token): Boolean {
        val account = accountManager.activeAccount ?: return false
        walletManager.save(listOf(Wallet(token, account)))
        return true
    }

    fun createWalletIfNotExists(token: Token): Wallet? =
        getWallet(token) ?: if (createWallet(token)) getWallet(token) else null

    fun getReceiveAddress(token: Token): String {
        val wallet = getWallet(token)
        requireNotNull(wallet) { "WalletUseCase: wallet for $token is not found" }
        val adapter = adapterManager.getReceiveAdapterForWallet(wallet)
        requireNotNull(adapter) { "WalletUseCase: adapter for $token is not found" }

        return adapter.receiveAddress
    }
}