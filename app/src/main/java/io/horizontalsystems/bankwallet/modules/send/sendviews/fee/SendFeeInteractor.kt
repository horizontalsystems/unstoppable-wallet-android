package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import io.horizontalsystems.bankwallet.core.IRateStorage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class SendFeeInteractor(private val rateStorage: IRateStorage) : SendFeeModule.IInteractor {

    var delegate: SendFeeModule.IInteractorDelegate? = null
    private var disposable: Disposable? = null

    override fun getRate(coinCode: String, currencyCode: String) {
        disposable = rateStorage.latestRateObservable(coinCode, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.onRateFetched(it)
                }
    }
}
