package io.horizontalsystems.bankwallet.modules.send.binance

import io.horizontalsystems.bankwallet.core.ISendBinanceAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendBinanceInteractor(private val adapter: ISendBinanceAdapter) : SendModule.ISendBinanceInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendBinanceInteractorDelegate? = null

    override val availableBalance: BigDecimal
        get() = adapter.availableBalance

    override val availableBinanceBalance: BigDecimal
        get() = adapter.availableBinanceBalance

    override val fee: BigDecimal
        get() = adapter.fee

    override fun validate(address: String) {
        adapter.validate(address)
    }

    override fun send(amount: BigDecimal, address: String, memo: String?) {
        adapter.send(amount, address, memo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    delegate?.didSend()
                }, { error ->
                    delegate?.didFailToSend(error)
                }).let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }

}
