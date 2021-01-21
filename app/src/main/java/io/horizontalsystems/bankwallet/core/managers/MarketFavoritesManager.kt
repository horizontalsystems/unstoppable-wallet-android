package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.FavoriteCoin
import io.horizontalsystems.bankwallet.core.storage.MarketFavoritesDao
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MarketFavoritesManager(appDatabase: AppDatabase) {

    val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    private val dao: MarketFavoritesDao by lazy {
        appDatabase.marketFavoritesDao()
    }

    fun add(coinCode: String) {
        dao.insert(FavoriteCoin(coinCode))
        dataUpdatedSubject.onNext(Unit)
    }

    fun remove(coinCode: String) {
        dao.delete(coinCode)
        dataUpdatedSubject.onNext(Unit)
    }

    fun getAll(): List<FavoriteCoin> {
        return dao.getAll()
    }

    fun isCoinInFavorites(coinCode: String): Boolean {
        return dao.getCount(coinCode) > 0
    }


}
