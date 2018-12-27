package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.Currency
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject


class CurrencyManager(
        private val localStorageManager: ILocalStorage,
        private val appConfigProvider: IAppConfigProvider) : ICurrencyManager {

    override val baseCurrency: Currency
        get() {
            val currencies = appConfigProvider.currencies
            val storedCode = localStorageManager.baseCurrencyCode
            storedCode?.let { code ->
                return currencies.first { it.code == code }
            }
            return currencies[0]
        }

    override var subject: BehaviorSubject<Currency> = BehaviorSubject.createDefault(baseCurrency)

    override val baseCurrencyObservable: Flowable<Currency> = subject.toFlowable(BackpressureStrategy.DROP)

    override val currencies: List<Currency>
        get() = appConfigProvider.currencies


    override fun setBaseCurrency(code: String) {
        localStorageManager.baseCurrencyCode = code
        subject.onNext(baseCurrency)
    }

}
