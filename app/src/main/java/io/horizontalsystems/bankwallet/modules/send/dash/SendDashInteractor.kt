package io.horizontalsystems.bankwallet.modules.send.dash

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ISendDashAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendDashInteractor(private val adapter: ISendDashAdapter) : SendModule.ISendDashInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendDashInteractorDelegate? = null

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

    override fun fetchMinimumAmount(address: String?): BigDecimal {
        return adapter.minimumSendAmount(address)
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

    override fun send(amount: BigDecimal, address: String, logger: AppLogger): Single<Unit> {
        return adapter.send(amount, address, logger)
    }

    override fun clear() {
        disposables.clear()
    }

}
