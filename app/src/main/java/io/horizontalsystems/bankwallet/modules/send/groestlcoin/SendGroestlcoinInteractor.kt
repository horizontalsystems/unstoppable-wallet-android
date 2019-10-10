package io.horizontalsystems.bankwallet.modules.send.groestlcoin

import io.horizontalsystems.bankwallet.core.ISendGroestlcoinAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendGroestlcoinInteractor(private val adapter: ISendGroestlcoinAdapter) : SendModule.ISendGroestlcoinInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendGroestlcoinInteractorDelegate? = null

    override fun fetchAvailableBalance(address: String?) {
        Single.just(adapter.availableBalance(address))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ availableBalance ->
                    delegate?.didFetchAvailableBalance(availableBalance)
                }, {

                })
                .let { disposables.add(it) }
    }

    override fun validate(address: String) {
        adapter.validate(address)
    }

    override fun fetchFee(amount: BigDecimal, address: String?) {
        Single.just(adapter.fee(amount, address))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ fee ->
                    delegate?.didFetchFee(fee)
                }, {

                })
                .let { disposables.add(it) }
    }

    override fun send(amount: BigDecimal, address: String): Single<Unit> {
        return adapter.send(amount, address)
    }

    override fun clear() {
        disposables.clear()
    }

}
