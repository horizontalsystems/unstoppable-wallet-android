package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.util.concurrent.Executors

class RatesRepository(private val appDatabase: AppDatabase) : IRateStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun rateObservable(coinCode: CoinCode, currencyCode: String): Flowable<Rate> {
        return appDatabase.ratesDao().getRateX(coinCode, currencyCode)
    }

    override fun rate(coinCode: CoinCode, currencyCode: String): Maybe<Rate> {
        return appDatabase.ratesDao().getRate(coinCode, currencyCode)
    }

    override fun save(latestRate: LatestRate, coinCode: CoinCode, currencyCode: String) {
        executor.execute {
            appDatabase.ratesDao().insert(Rate(coinCode, currencyCode, latestRate.value, latestRate.timestamp))
        }
    }

    override fun getAll(): Flowable<List<Rate>> {
        return appDatabase.ratesDao().getAll()
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.ratesDao().deleteAll()
        }
    }

}
