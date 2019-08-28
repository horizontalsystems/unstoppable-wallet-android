package io.horizontalsystems.bankwallet.modules.send

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendInteractor : SendModule.ISendInteractor {
    private var disposables = CompositeDisposable()

    override lateinit var delegate: SendModule.ISendInteractorDelegate

    override fun send(sendSingle: Single<Unit>) {
        sendSingle.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate.didSend()
                }, { error ->
                    delegate.didFailToSend(error)
                }).let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }

}
