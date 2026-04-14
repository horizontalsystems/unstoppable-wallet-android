package com.quantum.wallet.bankwallet.core.storage

import com.quantum.wallet.bankwallet.core.IEnabledWalletStorage
import com.quantum.wallet.bankwallet.entities.EnabledWallet

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
