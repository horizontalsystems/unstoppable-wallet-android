package io.horizontalsystems.marketkit.syncers

import android.util.Log
import io.horizontalsystems.marketkit.providers.HsProvider
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class HsDataSyncer(
    private val coinSyncer: CoinSyncer,
    private val hsProvider: HsProvider,
) {

    private var disposable: Disposable? = null

    fun sync() {
        disposable = hsProvider.statusSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({ status ->
                coinSyncer.sync(status.coins, status.blockchains, status.tokens)
            }, {
                Log.e("CoinSyncer", "sync() error", it)
            })
    }

}
