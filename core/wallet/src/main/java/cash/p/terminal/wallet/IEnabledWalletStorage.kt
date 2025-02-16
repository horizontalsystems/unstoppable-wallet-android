package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.EnabledWallet

interface IEnabledWalletStorage {
    val enabledWallets: List<EnabledWallet>
    fun enabledWallets(accountId: String): List<EnabledWallet>
    fun save(enabledWallets: List<EnabledWallet>)
    fun delete(enabledWalletIds: List<Long>)
    fun deleteAll()
}