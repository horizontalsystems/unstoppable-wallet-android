package bitcoin.wallet.core

import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.viewHelpers.DateHelper
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ExchangeRateManager: IExchangeRateManager {

    private var disposables: CompositeDisposable = CompositeDisposable()
    private var baseCurrencyDisposable: Disposable? = null

    init {
        baseCurrencyDisposable = Factory.currencyManager.getBaseCurrencyFlowable().subscribe { baseCurrency ->
            disposables.clear()
            disposables.add(Observable.interval(0, 5, TimeUnit.MINUTES, Schedulers.io())
                    .subscribe {
                        refreshRates(baseCurrency)
                    })
        }
    }

    private var latestExchangeRateSubject: PublishSubject<MutableMap<Coin, CurrencyValue>> = PublishSubject.create()

    override fun getLatestExchangeRateSubject() = latestExchangeRateSubject

    private var exchangeRates: MutableMap<Coin, CurrencyValue> = hashMapOf()

    override fun getExchangeRates() = exchangeRates

    private fun refreshRates(baseCurrency: Currency) {
        val flowableList = mutableListOf<Flowable<Pair<String, Double>>>()
        AdapterManager.adapters.forEach {adapter ->
            flowableList.add(Factory.networkManager.getLatestRate(adapter.coin.code, baseCurrency.code)
                    .map { Pair(adapter.coin.code, it) })
        }

        disposables.add(Flowable.zip(flowableList, Arrays::asList)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .map {resultRates ->
                    (resultRates as List<Pair<String, Double>>).toMap()
                }
                .subscribe { ratesMap ->
                    AdapterManager.adapters.forEach {adapter ->
                        val rate = ratesMap[adapter.coin.code] ?: 0.0
                        exchangeRates[adapter.coin] = CurrencyValue(baseCurrency, rate)
                    }
                    latestExchangeRateSubject.onNext(exchangeRates)
                })
    }

    override fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double> {
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

        return Factory.networkManager.getRate(coinCode, currency, year, formattedMonth, formattedDay, formattedHour, formattedMinute)
    }

}
