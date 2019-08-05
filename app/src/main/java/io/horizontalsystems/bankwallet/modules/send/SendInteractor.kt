package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.WrongParameters
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal


class SendInteractor(private val adapter: IAdapter) : SendModule.IInteractor {

    sealed class SendError : Exception() {
        class NoAddress : SendError()
        class NoAmount : SendError()
    }

    var delegate: SendModule.IInteractorDelegate? = null

    override val coin: Coin
        get() = adapter.wallet.coin

    private var validateDisposable: Disposable? = null
    private var feeDisposable: Disposable? = null
    private var sendDisposable: Disposable? = null

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return adapter.parsePaymentAddress(address)
    }

    @Throws
    override fun getAvailableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return adapter.availableBalance(params)
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>) {
        validateDisposable?.dispose()

        validateDisposable = Single.fromCallable { adapter.validate(params) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { errorList -> delegate?.onValidationComplete(errorList) },
                        { /*exception*/ }
                )
    }

    override fun updateFee(params: Map<SendModule.AdapterFields, Any?>) {
        feeDisposable?.dispose()

        feeDisposable = Single.fromCallable { adapter.fee(params) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { fee ->
                            delegate?.onFeeUpdated(fee)
                        },
                        { error ->
                            /*exception*/
                        }
                )
    }

    override fun send(params: Map<SendModule.AdapterFields, Any?>) {
        try {
            sendDisposable = adapter.send(params)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { delegate?.didSend() },
                            { error ->
                                delegate?.showError(error)
                            })
        } catch (error: WrongParameters) {
            //todo add proper error text for this error
            delegate?.showError(error)
        }
    }

    override fun clear() {
        validateDisposable?.dispose()
        feeDisposable?.dispose()
        sendDisposable?.dispose()
    }

}
