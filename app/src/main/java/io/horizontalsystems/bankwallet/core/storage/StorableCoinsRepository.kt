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
            //update stored coins enabled as false, and order as null
            appDatabase.coinsDao().resetCoinsState()

            //save coin list
            coins.forEachIndexed { index, coin ->
                val storableCoin = StorableCoin(
                        coinCode = coin.code,
                        coinTitle = coin.title,
                        coinType = coin.type,
                        enabled = true,
                        order = index)
                appDatabase.coinsDao().insert(storableCoin)
            }
        }
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.coinsDao().deleteAll()
        }
    }

}
