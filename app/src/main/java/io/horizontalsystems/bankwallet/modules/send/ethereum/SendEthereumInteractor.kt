package io.horizontalsystems.bankwallet.modules.send.ethereum

import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendEthereumInteractor(private val adapter: ISendEthereumAdapter) : SendModule.ISendEthereumInteractor {
    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendEthereumInteractorDelegate? = null

    override val ethereumBalance: BigDecimal
        get() = adapter.ethereumBalance

    override fun availableBalance(gasPrice: Long): BigDecimal {
        return adapter.availableBalance(gasPrice)
    }

    override fun validate(address: String) {
        adapter.validate(address)
    }

    override fun fee(gasPrice: Long): BigDecimal {
        return adapter.fee(gasPrice)
    }

    override fun send(amount: BigDecimal, address: String, gasPrice: Long) {
        adapter.send(amount, address, gasPrice)
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
