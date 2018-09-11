package bitcoin.wallet.core

import io.reactivex.subjects.PublishSubject

object ExchangeRateManager {

    var subject: PublishSubject<MutableMap<String, Double>> = PublishSubject.create()

    var exchangeRates: Map<String, Double> = hashMapOf("BTC" to 1000.0)

    fun updateRates() {
        //for testing purpose
        subject.onNext(hashMapOf("BTC" to 3000.0))
    }
}
