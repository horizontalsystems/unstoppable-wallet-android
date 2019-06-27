package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IEnabledCoinStorage
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.Flowable
import java.util.concurrent.Executors

class EnabledCoinsRepository(private val appDatabase: AppDatabase) : IEnabledCoinStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun enabledCoinsObservable(): Flowable<List<EnabledWallet>> {
        return appDatabase.walletsDao().getEnabledCoins()
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
