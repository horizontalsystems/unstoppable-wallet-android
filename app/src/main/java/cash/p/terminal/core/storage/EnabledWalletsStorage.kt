package cash.p.terminal.core.storage

import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.entities.EnabledWallet

class EnabledWalletsStorage(private val appDatabase: AppDatabase) : IEnabledWalletStorage {

    override val enabledWallets: List<EnabledWallet>
        get() = appDatabase.walletsDao().enabledCoins()

    override fun enabledWallets(accountId: String): List<EnabledWallet> {
        return appDatabase.walletsDao().enabledCoins(accountId)
    }

    override fun save(enabledWallets: List<EnabledWallet>) {
        appDatabase.walletsDao().insertWallets(enabledWallets)
    }

    override fun deleteAll() {
        appDatabase.walletsDao().deleteAll()
    }

    override fun delete(enabledWallets: List<EnabledWallet>) {
        val tokenQueryIds = enabledWallets.map { it.tokenQueryId.lowercase() }
        val accountIds = enabledWallets.map { it.accountId.lowercase() }
        appDatabase.walletsDao().deleteWallets(
            tokenQueryIds = tokenQueryIds,
            accountIds = accountIds
        )
    }
}
