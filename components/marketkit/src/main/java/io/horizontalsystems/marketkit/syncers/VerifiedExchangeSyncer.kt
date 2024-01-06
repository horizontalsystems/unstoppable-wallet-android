package io.horizontalsystems.marketkit.syncers

import android.util.Log
import io.horizontalsystems.marketkit.managers.ExchangeManager
import io.horizontalsystems.marketkit.models.VerifiedExchange
import io.horizontalsystems.marketkit.providers.HsProvider
import io.horizontalsystems.marketkit.storage.SyncerStateDao
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class VerifiedExchangeSyncer(
    private val exchangeManager: ExchangeManager,
    private val hsProvider: HsProvider,
    private val syncerStateDao: SyncerStateDao
) {
    private val keyLastSyncTimestamp = "verified-exchange-syncer-last-sync-timestamp"
    private var disposable: Disposable? = null

    fun sync(timestamp: Long) {
        val lastSyncTimestamp = syncerStateDao.get(keyLastSyncTimestamp)?.toLong() ?: 0

        if (lastSyncTimestamp == timestamp) return

        disposable = hsProvider.verifiedExchangeUids()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({ verifiedExchangeUids ->
                exchangeManager.handleFetchedVerified(verifiedExchangeUids.map { VerifiedExchange(it) })
                syncerStateDao.save(keyLastSyncTimestamp, timestamp.toString())
            }, {
                Log.e("VerifiedExchangeSyncer", "Fetch error", it)
            })
    }

    fun stop() {
        disposable?.dispose()
        disposable = null
    }

}
