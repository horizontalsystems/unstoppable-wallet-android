package bitcoin.wallet.core

import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.viewHelpers.DateHelper
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ExchangeRateManager: IExchangeRateManager {

    init {
        val disposable = Observable.interval(0, 5, TimeUnit.MINUTES, Schedulers.io())
                .subscribe {
                    refreshRates()
                }
    }

    var latestExchangeRateSubject: PublishSubject<Map<String, Double>> = PublishSubject.create()

    var exchangeRates: MutableMap<String, Double> = hashMapOf("BTC" to 0.0)

    override fun refreshRates() {
        val coinCode = "btc"
        val currency = "usd"
        val disposable = Factory.networkManager.getLatestRate(coinCode.toLowerCase(), currency.toLowerCase())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe { rate ->
                        val rateAsDouble = rate.toDouble()
                        exchangeRates[coinCode.toUpperCase()] = rateAsDouble
                        latestExchangeRateSubject.onNext(hashMapOf(coinCode.toUpperCase() to rateAsDouble))
                }
    }

    override fun getRate(coinCode: String, currency: String, timestamp: Long) : Flowable<Double> {
        val calendar = DateHelper.getCalendarFromTimestamp(timestamp)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val f = DecimalFormat("00")
        val formattedMonth = f.format(month).toString()
        val formattedDay = f.format(day).toString()
        val formattedHour = f.format(hour).toString()
        val formattedMinute = f.format(minute).toString()

        return Factory.networkManager.getRate(coinCode.toLowerCase(), currency.toLowerCase(), year, formattedMonth, formattedDay, formattedHour, formattedMinute)
    }

}
