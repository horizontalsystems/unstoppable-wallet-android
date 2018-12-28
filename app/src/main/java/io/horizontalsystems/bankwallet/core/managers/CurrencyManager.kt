package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.Currency
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.Executors


class CurrencyManager(private val localStorage: ILocalStorage, private val appConfigProvider: IAppConfigProvider) : ICurrencyManager {

    @Deprecated("")
    override val baseCurrency: Currency
        get() {
            val currencies = appConfigProvider.currencies
            val storedCode = localStorage.baseCurrencyCode
            storedCode?.let { code ->
                return currencies.first { it.code == code }
            }
            return currencies[0]
        }

    @Deprecated("")
    override var subject: BehaviorSubject<Currency> = BehaviorSubject.create()

    override val currencies: List<Currency>
        get() = appConfigProvider.currencies

    @Deprecated("")
    override fun setBaseCurrency(code: String) {
        localStorage.baseCurrencyCode = code
        subject.onNext(baseCurrency)
        setBaseCurrency2(code)
    }


    private var subject2: BehaviorSubject<Currency> = BehaviorSubject.create()

    override val baseCurrencyObservable: Flowable<Currency> = subject2.toFlowable(BackpressureStrategy.DROP)

    private val executorService = Executors.newCachedThreadPool()

    init {
        executorService.execute {
            notifyUpdate(localStorage.baseCurrencyCode)
        }
    }

    fun setBaseCurrency2(code: String) {
        executorService.execute {
            localStorage.baseCurrencyCode = code
            notifyUpdate(code)
        }
    }

    private fun notifyUpdate(currencyCode: String?) {
        currencies.find { it.code == currencyCode } ?: currencies.firstOrNull()

        val currency = currencyCode?.let { currencyCode ->
            currencies.find { it.code == currencyCode }
        } ?: currencies.firstOrNull()

        currency?.let { subject2.onNext(it) }
    }
}
