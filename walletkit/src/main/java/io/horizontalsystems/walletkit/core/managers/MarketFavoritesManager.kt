package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.storage.AppDatabase
import io.horizontalsystems.walletkit.core.storage.FavoriteCoin
import io.horizontalsystems.walletkit.core.storage.MarketFavoritesDao
import io.horizontalsystems.walletkit.widgets.MarketWidgetManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MarketFavoritesManager(
    appDatabase: AppDatabase,
    private val localStorage: ILocalStorage,
    private val marketWidgetManager: MarketWidgetManager
) {
    val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    private val dao: MarketFavoritesDao by lazy {
        appDatabase.marketFavoritesDao()
    }

    fun add(coinUid: String) {
        localStorage.marketFavoritesManualSortingOrder =
            localStorage.marketFavoritesManualSortingOrder.toMutableList().apply {
                add(coinUid)
            }
        dao.insert(FavoriteCoin(coinUid))
        dataUpdatedSubject.onNext(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun addAll(coinUids: List<String>) {
        dao.insertAll(coinUids.map { FavoriteCoin(it) })
        dataUpdatedSubject.onNext(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun remove(coinUid: String) {
        localStorage.marketFavoritesManualSortingOrder =
            localStorage.marketFavoritesManualSortingOrder.toMutableList().apply {
                remove(coinUid)
            }
        dao.delete(coinUid)
        dataUpdatedSubject.onNext(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun getAll(): List<FavoriteCoin> {
        return dao.getAll()
    }

    fun isCoinInFavorites(coinUid: String): Boolean {
        return dao.getCount(coinUid) > 0
    }
}
