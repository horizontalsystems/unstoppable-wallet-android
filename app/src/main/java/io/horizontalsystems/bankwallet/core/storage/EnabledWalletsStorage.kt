package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IEnabledWalletStorage
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.Flowable
import java.util.concurrent.Executors

class EnabledWalletsStorage(private val appDatabase: AppDatabase) : IEnabledWalletStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override val enabledWallets: List<EnabledWallet>
        get() = appDatabase.walletsDao().enabledCoins()

    override fun enabledWalletsFlowable(): Flowable<List<EnabledWallet>> {
        return appDatabase.walletsDao().enabledCoinsFlowable()
    }

    override fun save(coins: List<EnabledWallet>) {
        executor.execute {
            appDatabase.walletsDao().deleteAll()
            appDatabase.walletsDao().insertCoins(coins)
        }
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.walletsDao().deleteAll()
        }
    }
}
