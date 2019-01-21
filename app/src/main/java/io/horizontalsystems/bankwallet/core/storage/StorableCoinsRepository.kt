package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.util.concurrent.Executors

class StorableCoinsRepository(private val appDatabase: AppDatabase) : ICoinStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun coinObservable(coinCode: CoinCode): Flowable<StorableCoin> {
        return appDatabase.coinsDao().getCoin(coinCode)
    }

    override fun save(storableCoin: StorableCoin) {
        executor.execute {
            appDatabase.coinsDao().insert(storableCoin)
        }
    }

    override fun getAll(): Flowable<List<StorableCoin>> {
        return appDatabase.coinsDao().getAll()
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.coinsDao().deleteAll()
        }
    }

}
