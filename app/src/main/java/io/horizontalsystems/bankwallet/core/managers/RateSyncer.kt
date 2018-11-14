package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateSyncerDelegate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateSyncer(private val networkManager: INetworkManager, private val timer: PeriodicTimer) {

    private var disposables: CompositeDisposable = CompositeDisposable()
    var delegate: IRateSyncerDelegate? = null

    fun sync(coins: List<String>, currencyCode: String) {
        disposables.clear()
        coins.forEach { coin ->
            disposables.add(networkManager.getLatestRate(coin = coin, currency = currencyCode)
                    .subscribeOn(Schedulers.io())
                    .unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { value ->
                        delegate?.didSync(coin = coin, currencyCode = currencyCode, value = value)
                    })
        }

        timer.schedule()
    }
}
