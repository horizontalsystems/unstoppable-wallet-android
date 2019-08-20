package io.horizontalsystems.bankwallet.modules.send.eos

import io.horizontalsystems.bankwallet.core.ISendEosAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendEosInteractor(private val adapter: ISendEosAdapter) : SendModule.ISendEosInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendEosInteractorDelegate? = null

    override val availableBalance: BigDecimal
        get() = adapter.availableBalance

    override fun validate(account: String) {
        adapter.validate(account)
    }

    override fun send(amount: BigDecimal, account: String, memo: String?) {
        adapter.send(amount, account, memo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didSend()
                }, { error ->
                    delegate?.didFailToSend(error)
                }).let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }

}
