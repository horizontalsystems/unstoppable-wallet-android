package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.core.subscribeAsync
import bitcoin.wallet.entities.ExchangeRate
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ExchangeRateManager(private val databaseManager: IDatabaseManager, private val networkManager: INetworkManager, private val updateSubject: PublishSubject<List<ExchangeRate>>) {

    private val compositeDisposable = CompositeDisposable()

    fun refresh() {
        networkManager.getExchangeRates().subscribeAsync(compositeDisposable, {
            databaseManager.truncateExchangeRates()
            databaseManager.insertExchangeRates(it)
            updateSubject.onNext(it)
        })
    }

}
