package io.horizontalsystems.walletkit.core.storage

import io.horizontalsystems.walletkit.core.IEnabledWalletStorage
import io.horizontalsystems.walletkit.entities.EnabledWallet

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
        appDatabase.walletsDao().deleteWallets(enabledWallets)
    }
}
