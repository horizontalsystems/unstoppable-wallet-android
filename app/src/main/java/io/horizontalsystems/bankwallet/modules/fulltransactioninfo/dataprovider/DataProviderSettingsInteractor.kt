package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.dataprovider

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.URL

class DataProviderSettingsInteractor(private val dataProviderManager: ITransactionDataProviderManager, val networkManager: INetworkManager)
    : DataProviderSettingsModule.Interactor {

    val disposables = CompositeDisposable()
    var delegate: DataProviderSettingsModule.InteractorDelegate? = null

    override fun pingProvider(name: String, url: String) {
        val uri = URL(url)
        val host = "${uri.protocol}://${uri.host}"

        disposables.add(networkManager.ping(host, url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.onPingSuccess(name)
                }, {
                    delegate?.onPingFailure(name)
                }))
    }

    override fun providers(coinCode: CoinCode): List<FullTransactionInfoModule.Provider> {
        return dataProviderManager.providers(coinCode)
    }

    override fun baseProvider(coinCode: CoinCode): FullTransactionInfoModule.Provider {
        return dataProviderManager.baseProvider(coinCode)
    }

    override fun setBaseProvider(name: String, coinCode: CoinCode) {
        dataProviderManager.setBaseProvider(name, coinCode)

        delegate?.onSetDataProvider()
    }
}
