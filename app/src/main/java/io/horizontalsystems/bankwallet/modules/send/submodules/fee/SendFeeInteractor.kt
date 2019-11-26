package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IFeeRateProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.address.SendAddressModule
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendFeeInteractor(
        private val rateManager: IRateManager,
        private val feeRateProvider: IFeeRateProvider?,
        private val currencyManager: ICurrencyManager)
    : SendFeeModule.IInteractor {

    var delegate: SendFeeModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun getRate(coinCode: String): BigDecimal? {
        return rateManager.getLatestRate(coinCode, currencyManager.baseCurrency.code)
    }

    override fun syncFeeRate() {
        if (feeRateProvider == null)
            return

        feeRateProvider.feeRates()
                    .subscribeOn(Schedulers.io())
                    .subscribe({ delegate?.didUpdate(it) }, { delegate?.didReceiveError(it) })
                    .let { disposables.add(it) }
    }

    override fun onClear() {
        disposables.dispose()
    }
}


