package bitcoin.wallet.core

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.entities.ExchangeRate
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExchangeRateService @Inject constructor(private val networkManager: INetworkManager, private val storage: BlockchainStorage) {

    fun start() {
        Observable.interval(0, 10, TimeUnit.MINUTES, Schedulers.io())
                .subscribe {
                    refreshRates()
                }
    }

    fun refreshRates() {
        networkManager.getExchangeRates()
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { rates ->
                    rates.forEach { rate ->
                        storage.updateExchangeRate(
                                ExchangeRate().apply {
                                    code = rate.key
                                    value = 1 / rate.value
                                }
                        )

                    }
                }
    }

}
