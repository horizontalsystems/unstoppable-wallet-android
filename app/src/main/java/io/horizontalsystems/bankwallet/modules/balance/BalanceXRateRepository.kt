package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class BalanceXRateRepository(
    private val currencyManager: ICurrencyManager,
    private val xRateManager: IRateManager
) {
    val baseCurrency by currencyManager::baseCurrency
    private var coinTypes = listOf<CoinType>()

    private var latestRateDisposable: Disposable? = null
    private var baseCurrencyDisposable: Disposable? = null

    private val itemSubject = PublishSubject.create<Map<CoinType, LatestRate?>>()
    val itemObservable: Observable<Map<CoinType, LatestRate?>> get() = itemSubject
        .doOnSubscribe {
            subscribeForBaseCurrencyUpdate()
            subscribeForLatestRateUpdates()
        }
        .doFinally {
            unsubscribeFromBaseCurrencyUpdate()
            unsubscribeFromLatestRateUpdates()
        }

    private fun subscribeForBaseCurrencyUpdate() {
        baseCurrencyDisposable = currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                unsubscribeFromLatestRateUpdates()
                itemSubject.onNext(coinTypes.map { it to latestRate(it) }.toMap())
                subscribeForLatestRateUpdates()
            }
    }

    private fun unsubscribeFromBaseCurrencyUpdate() {
        baseCurrencyDisposable?.dispose()
    }

    fun setCoinTypes(coinTypes: List<CoinType>) {
        unsubscribeFromLatestRateUpdates()
        this.coinTypes = coinTypes
        subscribeForLatestRateUpdates()
    }

    fun latestRate(coinType: CoinType) = xRateManager.latestRate(coinType, baseCurrency.code)

    fun refresh() {
        xRateManager.refresh(baseCurrency.code)
    }

    private fun subscribeForLatestRateUpdates() {
        latestRateDisposable = xRateManager.latestRateObservable(coinTypes, baseCurrency.code)
            .subscribeIO {
                itemSubject.onNext(it)
            }
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRateDisposable?.dispose()
    }
}