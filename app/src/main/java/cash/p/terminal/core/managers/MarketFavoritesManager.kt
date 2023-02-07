package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.core.storage.FavoriteCoin
import cash.p.terminal.core.storage.MarketFavoritesDao
import cash.p.terminal.widgets.MarketWidgetManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MarketFavoritesManager(
    appDatabase: AppDatabase,
    private val marketWidgetManager: MarketWidgetManager
) {
    val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    private val dao: MarketFavoritesDao by lazy {
        appDatabase.marketFavoritesDao()
    }

    fun add(coinUid: String) {
        dao.insert(FavoriteCoin(coinUid))
        dataUpdatedSubject.onNext(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun remove(coinUid: String) {
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
