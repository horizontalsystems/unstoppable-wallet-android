package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.INetworkManager
import bitcoin.wallet.core.subscribeAsync
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class UnspentOutputManager(private val databaseManager: IDatabaseManager, private val networkManager: INetworkManager, private val updateSubject: PublishSubject<List<UnspentOutput>>) {

    private val compositeDisposable = CompositeDisposable()

    fun refresh() {
        networkManager.getUnspentOutputs().subscribeAsync(compositeDisposable, {
            databaseManager.truncateUnspentOutputs()
            databaseManager.insertUnspentOutputs(it)
            updateSubject.onNext(it)
        })
    }

}
