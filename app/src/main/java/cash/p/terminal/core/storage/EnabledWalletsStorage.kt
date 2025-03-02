package cash.p.terminal.core.storage

import cash.p.terminal.wallet.IEnabledWalletStorage
import cash.p.terminal.wallet.entities.EnabledWallet

class EnabledWalletsStorage(private val appDatabase: AppDatabase) : IEnabledWalletStorage {

    override val enabledWallets: List<EnabledWallet>
        get() = appDatabase.walletsDao().enabledCoins()

    override fun enabledWallets(accountId: String): List<EnabledWallet> {
        return appDatabase.walletsDao().enabledCoins(accountId)
    }

    override fun save(enabledWallets: List<EnabledWallet>): List<Long> =
        appDatabase.walletsDao().insertWallets(enabledWallets)

    override fun deleteAll() {
        appDatabase.walletsDao().deleteAll()
    }

    override fun delete(enabledWalletIds: List<Long>) {
        appDatabase.walletsDao().deleteWallets(enabledWalletIds)
    }
}
