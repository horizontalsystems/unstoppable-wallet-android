package io.horizontalsystems.bankwallet.core.fiat

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AmountTypeSwitchService {

    interface IToggleAvailableListener {
        val toggleAvailable: Boolean
        val toggleAvailableObservable: Observable<Boolean>
    }

    private val disposables = CompositeDisposable()

    private val amountTypeSubject = PublishSubject.create<AmountType>()
    var amountType: AmountType = AmountType.Coin
        private set(value) {
            field = value
            amountTypeSubject.onNext(value)
        }
    val amountTypeObservable: Observable<AmountType>
        get() = amountTypeSubject


    private val toggleAvailableSubject = PublishSubject.create<Boolean>()
    var toggleAvailable: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                toggleAvailableSubject.onNext(value)
            }
        }
    val toggleAvailableObservable: Observable<Boolean>
        get() = toggleAvailableSubject


    var fromListener: IToggleAvailableListener? = null
        set(value) {
            field = value
            value?.let { fromListener ->
                fromListener.toggleAvailableObservable
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            syncToggleAvailable()
                        }
                        .let { disposables.add(it) }
            }
        }

    var toListener: IToggleAvailableListener? = null
        set(value) {
            field = value
            value?.let { toListener ->
                toListener.toggleAvailableObservable
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            syncToggleAvailable()
                        }
                        .let { disposables.add(it) }
            }
        }

    private fun syncToggleAvailable() {
        toggleAvailable = (fromListener?.toggleAvailable ?: false) &&
                (toListener?.toggleAvailable ?: false)
        if (!toggleAvailable && amountType == AmountType.Currency) {
            amountType = AmountType.Coin
        }
    }

    fun toggle() {
        if (toggleAvailable) {
            amountType = if (amountType == AmountType.Coin) AmountType.Currency else AmountType.Coin
        }
    }

    enum class AmountType {
        Coin, Currency
    }

}
