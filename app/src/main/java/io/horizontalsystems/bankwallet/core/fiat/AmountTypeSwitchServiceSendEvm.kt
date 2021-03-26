package io.horizontalsystems.bankwallet.core.fiat

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class AmountTypeSwitchServiceSendEvm(
        amountType: AmountType = AmountType.Coin
) : Clearable {
    private var disposable: Disposable? = null
    private val toggleAvailableObservables = mutableListOf<Flowable<Boolean>>()

    private val amountTypeSubject = PublishSubject.create<AmountType>()
    var amountType: AmountType = amountType
        private set(value) {
            field = value
            amountTypeSubject.onNext(value)
        }
    val amountTypeObservable: Flowable<AmountType>
        get() = amountTypeSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val toggleAvailableSubject = PublishSubject.create<Boolean>()
    var toggleAvailable: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                toggleAvailableSubject.onNext(value)
            }
        }
    val toggleAvailableObservable: Flowable<Boolean>
        get() = toggleAvailableSubject.toFlowable(BackpressureStrategy.BUFFER)

    private fun subscribeToObservables() {
        disposable?.dispose()

        Flowable.combineLatest(toggleAvailableObservables) { array ->
            array.map { it as Boolean }
        }.subscribeIO { list ->
            syncToggleAvailable(list)
        }.let { disposable = it }
    }

    private fun syncToggleAvailable(list: List<Boolean>) {
        toggleAvailable = list.all { it }

        if (!toggleAvailable && amountType == AmountType.Currency) {
            // reset input type if it was set to currency
            amountType = AmountType.Coin
        }

    }

    fun toggle() {
        if (toggleAvailable) {
            amountType = amountType.toggle()
        }
    }

    fun add(toggleAvailableObservable: Flowable<Boolean>) {
        toggleAvailableObservables.add(toggleAvailableObservable)
        subscribeToObservables()
    }

    override fun clear() {
        disposable?.dispose()
    }

    enum class AmountType {
        Coin, Currency;

        fun toggle(): AmountType {
            return when (this) {
                Coin -> Currency
                Currency -> Coin
            }
        }
    }

}
