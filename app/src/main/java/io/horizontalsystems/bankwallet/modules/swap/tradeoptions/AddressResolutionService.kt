package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.Address
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class AddressResolutionService(val coinCode: String, val isResolutionEnabled: Boolean = true) : Clearable {

    val isResolvingAsync = BehaviorSubject.createDefault(false)
    val resolveFinishedAsync = BehaviorSubject.createDefault<Optional<Address>>(Optional.empty())

    private val provider = AddressResolutionProvider()
    private var disposable: Disposable? = null

    var isResolving: Boolean = false
        private set(value) {
            if (field != value) {
                isResolvingAsync.onNext(value)
            }
            field = value
        }

    fun setText(text: String?) {
        if (!isResolutionEnabled) {
            return
        }

        if (text == null || !provider.isValid(text)) {
            isResolving = false
            return
        }

        isResolving = true

        disposable?.dispose()
        disposable = provider.resolveAsync(domain = text, ticker = coinCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ addressResponse ->
                    isResolving = false
                    resolveFinishedAsync.onNext(Optional.of(Address(hex = addressResponse, domain = text)))
                }, {
                    isResolving = false
                    resolveFinishedAsync.onNext(Optional.empty())
                })
    }

    override fun clear() {
        disposable?.dispose()
    }
}