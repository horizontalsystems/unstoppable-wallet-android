package bitcoin.wallet.core

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.entities.ExchangeRate
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object ExchangeRateService {
    lateinit var networkManager: INetworkManager

    fun start(storage: BlockchainStorage) {
        Observable.interval(0, 10, TimeUnit.MINUTES, Schedulers.io())
                .subscribe {
                    refreshRates(storage)
                }
    }

    fun refreshRates(storage: BlockchainStorage) {
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
