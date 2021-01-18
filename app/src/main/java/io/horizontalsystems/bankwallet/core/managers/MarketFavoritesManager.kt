package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.core.storage.FavoriteCoin
import io.horizontalsystems.bankwallet.core.storage.MarketFavoritesDao
import io.horizontalsystems.bankwallet.entities.CoinType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MarketFavoritesManager(appDatabase: AppDatabase) {

    val dataUpdatedAsync: Observable<Unit>
        get() = dataUpdatedSubject

    private val dataUpdatedSubject = PublishSubject.create<Unit>()

    private val dao: MarketFavoritesDao by lazy {
        appDatabase.marketFavoritesDao()
    }

    fun add(coinCode: String, coinType: CoinType?) {
        dao.insert(FavoriteCoin(coinCode, coinType))
        dataUpdatedSubject.onNext(Unit)
    }

    fun remove(coinCode: String, coinType: CoinType?) {
        dao.delete(coinCode, coinType)
        dataUpdatedSubject.onNext(Unit)
    }

    fun getAll(): List<FavoriteCoin> {
        return dao.getAll()
    }

    fun isCoinInFavorites(coinCode: String, coinType: CoinType?): Boolean {
        return dao.getCount(coinCode, coinType) > 0
    }


}
