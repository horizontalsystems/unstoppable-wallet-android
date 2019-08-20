package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class SendFeeInteractor(private val rateStorage: IRateStorage,
                        private val feeRateProvider: IFeeRateProvider?,
                        private val currencyManager: ICurrencyManager) : SendFeeModule.IInteractor {

    private var disposable: Disposable? = null

    var delegate: SendFeeModule.IInteractorDelegate? = null

    override fun getRate(coinCode: String) {
        disposable = rateStorage.latestRateObservable(coinCode, currencyManager.baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    delegate?.onRateFetched(it)
                }
    }

    override fun getFeeRate(feeRatePriority: FeeRatePriority): Long {
        return feeRateProvider?.feeRate(feeRatePriority) ?: 0
    }

    override fun clear() {
        disposable?.dispose()
    }

}
