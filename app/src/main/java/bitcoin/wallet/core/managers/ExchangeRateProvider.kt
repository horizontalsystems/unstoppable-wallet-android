package bitcoin.wallet.core.managers

import android.os.Handler
import bitcoin.wallet.core.IExchangeRateProvider
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.Coin
import io.reactivex.subjects.PublishSubject

class ExchangeRateProvider : IExchangeRateProvider {

    override val subject: PublishSubject<HashMap<Coin, Double>> = PublishSubject.create<HashMap<Coin, Double>>()

    override fun getExchangeRateForCoin(bitcoin: Coin): Double {
        return 7200.0
    }

    init {
        val handler = Handler()
        handler.postDelayed(Runnable {
            subject.onNext(
                    hashMapOf(Bitcoin() to 14_400.0)
            )

        }, 6000)

    }

}
