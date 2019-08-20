package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.ISendEosAdapter
import io.reactivex.disposables.CompositeDisposable
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
        adapter.send(amount, account, memo).subscribe({
            delegate?.didSend()
        }, { error ->
            delegate?.didFailToSend(error)
        }).let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }

}
