package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.EnabledWallet

interface IWalletStorage {
    fun wallets(account: Account): List<Wallet>
    fun save(wallets: List<Wallet>)
    fun delete(wallets: List<Wallet>)
    fun handle(newEnabledWallets: List<EnabledWallet>)
    fun clear()
}