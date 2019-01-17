package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import java.util.concurrent.Executors

class CoinsRepository(private val appDatabase: AppDatabase) : ICoinStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun coinObservable(coinCode: CoinCode): Flowable<Coin> {
        return appDatabase.coinsDao().getCoin(coinCode)
    }

    override fun save(coin: Coin) {
        executor.execute {
            appDatabase.coinsDao().insert(coin)
        }
    }

    override fun getAll(): Flowable<List<Coin>> {
        return appDatabase.coinsDao().getAll()
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.coinsDao().deleteAll()
        }
    }

}
