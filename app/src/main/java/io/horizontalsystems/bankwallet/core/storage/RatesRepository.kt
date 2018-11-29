package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.Coin
import io.reactivex.Maybe
import java.util.concurrent.Executors

class RatesRepository(private val appDatabase: AppDatabase) : IRateStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun rate(coin: Coin, currencyCode: String): Maybe<Rate> {
        return appDatabase.ratesDao().getRate(coin, currencyCode)
    }

    override fun save(latestRate: LatestRate, coin: Coin, currencyCode: String) {
        executor.execute {
            appDatabase.ratesDao().insert(Rate(coin, currencyCode, latestRate.value, latestRate.timestamp))
        }
    }

    override fun deleteAll() {
        executor.execute {
            appDatabase.ratesDao().deleteAll()
        }
    }

}
