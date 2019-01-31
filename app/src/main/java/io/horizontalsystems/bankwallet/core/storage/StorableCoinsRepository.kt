package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable
import java.util.concurrent.Executors

class StorableCoinsRepository(private val appDatabase: AppDatabase) : ICoinStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun enabledCoinsObservable(): Flowable<List<Coin>> {
        return appDatabase.coinsDao().getEnabledCoin()
                .flatMap { list ->
                    Flowable.just(list.map { Coin(it.coinTitle, it.coinCode, it.coinType) })
                }
    }

    override fun allCoinsObservable(): Flowable<List<Coin>> {
        return appDatabase.coinsDao().getAllCoins()
                .flatMap { list ->
                    Flowable.just(list.map { Coin(it.coinTitle, it.coinCode, it.coinType) })
                }
    }

    override fun save(coins: List<Coin>) {
        executor.execute {
            appDatabase.coinsDao().setEnabledCoins(coins.mapIndexed { index, coin ->
                StorableCoin(coin.code, coin.title, coin.type, true, index)
            })
        }
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.coinsDao().deleteAll()
        }
    }

}
