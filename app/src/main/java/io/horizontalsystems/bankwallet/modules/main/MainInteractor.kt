package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.reactivex.disposables.CompositeDisposable

class MainInteractor(
        private val rateAppManager: IRateAppManager)
    : MainModule.IInteractor {

    var delegate: MainModule.IInteractorDelegate? = null
    val disposables = CompositeDisposable()

    override fun onStart() {
        rateAppManager.showRateAppObservable
                .subscribe {
                    delegate?.didShowRateApp()
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }
}
