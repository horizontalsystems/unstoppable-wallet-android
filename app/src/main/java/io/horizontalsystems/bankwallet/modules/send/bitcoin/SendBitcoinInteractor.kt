package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendBitcoinInteractor(private val adapter: ISendBitcoinAdapter) : SendModule.ISendBitcoinInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendBitcoinInteractorDelegate? = null

    override fun fetchAvailableBalance(feeRate: Long, address: String?) {
        Single.just(adapter.availableBalance(feeRate, address))
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

    override fun fetchFee(amount: BigDecimal, feeRate: Long, address: String?) {
        Single.just(adapter.fee(amount, feeRate, address))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ fee ->
                    delegate?.didFetchFee(fee)
                }, {

                })
                .let { disposables.add(it) }
    }

    override fun send(amount: BigDecimal, address: String, feeRate: Long): Single<Unit> {
        return adapter.send(amount, address, feeRate)
    }

    override fun clear() {
        disposables.clear()
    }
}
