package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendAmountInteractor(
        private val baseCurrency: Currency,
        private val rateManager: IRateManager,
        private val localStorage: ILocalStorage,
        private val coin: Coin,
        private val backgroundManager: BackgroundManager)
    : SendAmountModule.IInteractor, BackgroundManager.Listener {

    private val disposables = CompositeDisposable()
    var delegate: SendAmountModule.IInteractorDelegate? = null

    init {
        backgroundManager.registerListener(this)

        rateManager.latestRateObservable(coin.type, baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { marketInfo ->
                    delegate?.didUpdateRate(marketInfo.rate)
                }
                .let {
                    disposables.add(it)
                }
    }

    override var defaultInputType: SendModule.InputType
        get() = localStorage.sendInputType ?: SendModule.InputType.COIN
        set(value) { localStorage.sendInputType = value }

    override fun getRate(): BigDecimal? {
        return rateManager.getLatestRate(coin.type, baseCurrency.code)
    }

    override fun willEnterForeground() {
        delegate?.willEnterForeground()
    }

    override fun onCleared() {
        disposables.clear()
        backgroundManager.unregisterListener(this)
    }

}
