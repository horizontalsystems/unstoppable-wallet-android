package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.AppLog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendInteractor : SendModule.ISendInteractor {
    private var disposables = CompositeDisposable()

    override lateinit var delegate: SendModule.ISendInteractorDelegate

    override fun send(sendSingle: Single<Unit>, actionId: String) {
        sendSingle.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    AppLog.log(actionId, "success")

                    delegate.didSend()
                }, { error ->
                    AppLog.log(actionId, "failed")

                    delegate.didFailToSend(error)
                }).let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }

}
