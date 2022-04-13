package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.isCustom
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendAmountInteractor(
    private val baseCurrency: Currency,
    private val marketKit: MarketKit,
    private val localStorage: ILocalStorage,
    private val platformCoin: PlatformCoin,
    private val backgroundManager: BackgroundManager)
    : SendAmountModule.IInteractor, BackgroundManager.Listener {

    private val disposables = CompositeDisposable()
    var delegate: SendAmountModule.IInteractorDelegate? = null

    init {
        backgroundManager.registerListener(this)

        if (!platformCoin.isCustom) {
            marketKit.coinPriceObservable(platformCoin.coin.uid, baseCurrency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { marketInfo ->
                    delegate?.didUpdateRate(marketInfo.value)
                }
                .let {
                    disposables.add(it)
                }
        }
    }

    override var defaultInputType: AmountInputType
        get() = localStorage.amountInputType ?: AmountInputType.COIN
        set(value) { localStorage.amountInputType = value }

    override fun getRate(): BigDecimal? {
        return marketKit.coinPrice(platformCoin.coin.uid, baseCurrency.code)?.value
    }

    override fun willEnterForeground() {
        delegate?.willEnterForeground()
    }

    override fun onCleared() {
        disposables.clear()
        backgroundManager.unregisterListener(this)
    }

}
