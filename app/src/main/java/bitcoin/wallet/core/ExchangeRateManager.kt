package bitcoin.wallet.core

import io.reactivex.subjects.PublishSubject

object ExchangeRateManager {

    var subject: PublishSubject<Map<String, Double>> = PublishSubject.create()

    var exchangeRates: Map<String, Double> = hashMapOf("BTC" to 6300.0)

    fun updateRates() {
        //for testing purpose
        subject.onNext(hashMapOf("BTC" to 7000.0))
    }
}
