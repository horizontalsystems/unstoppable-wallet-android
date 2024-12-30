package cash.p.terminal.core.storage

import cash.p.terminal.wallet.IEnabledWalletStorage

class EnabledWalletsStorage(private val appDatabase: AppDatabase) : IEnabledWalletStorage {

    override val enabledWallets: List<cash.p.terminal.wallet.entities.EnabledWallet>
        get() = appDatabase.walletsDao().enabledCoins()

    override fun enabledWallets(accountId: String): List<cash.p.terminal.wallet.entities.EnabledWallet> {
        return appDatabase.walletsDao().enabledCoins(accountId)
    }

    override fun save(enabledWallets: List<cash.p.terminal.wallet.entities.EnabledWallet>) {
        appDatabase.walletsDao().insertWallets(enabledWallets)
    }

    override fun deleteAll() {
        appDatabase.walletsDao().deleteAll()
    }

    override fun delete(enabledWallets: List<cash.p.terminal.wallet.entities.EnabledWallet>) {
        appDatabase.walletsDao().deleteWallets(enabledWallets)
    }
}
