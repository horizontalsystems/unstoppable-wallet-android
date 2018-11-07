package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IAppConfigProvider
import bitcoin.wallet.core.ICurrencyManager
import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.entities.Currency
import io.reactivex.subjects.PublishSubject


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

    override val currencies: List<Currency>
        get() = appConfigProvider.currencies

    override var subject: PublishSubject<Currency> = PublishSubject.create()

    override fun setBaseCurrency(code: String) {
        localStorageManager.baseCurrencyCode = code
        subject.onNext(baseCurrency)
    }

}
