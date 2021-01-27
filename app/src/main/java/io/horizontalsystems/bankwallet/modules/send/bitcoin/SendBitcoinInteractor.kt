package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class SendBitcoinInteractor(
        private val adapter: ISendBitcoinAdapter,
        private val storage: ILocalStorage)
    : SendModule.ISendBitcoinInteractor {

    private val disposables = CompositeDisposable()

    var delegate: SendModule.ISendBitcoinInteractorDelegate? = null

    override val isLockTimeEnabled: Boolean
        get() = storage.isLockTimeEnabled

    override val balance = adapter.balance

    override fun fetchAvailableBalance(feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?) {
        Single.just(adapter.availableBalance(feeRate, address, pluginData))
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

    override fun fetchMaximumAmount(pluginData: Map<Byte, IPluginData>): BigDecimal? {
        return adapter.maximumSendAmount(pluginData)
    }

    override fun validate(address: String, pluginData: Map<Byte, IPluginData>?) {
        adapter.validate(address, pluginData)
    }

    override fun fetchFee(amount: BigDecimal, feeRate: Long, address: String?, pluginData: Map<Byte, IPluginData>?) {
        Single.just(adapter.fee(amount, feeRate, address, pluginData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ fee ->
                               delegate?.didFetchFee(fee)
                           }, {

                           })
                .let { disposables.add(it) }
    }

    override fun send(amount: BigDecimal, address: String, feeRate: Long,
                      pluginData: Map<Byte, IPluginData>?, logger: AppLogger): Single<Unit> {
        return adapter.send(amount, address, feeRate, pluginData, storage.transactionSortingType, logger)
    }

    override fun clear() {
        disposables.clear()
    }
}
