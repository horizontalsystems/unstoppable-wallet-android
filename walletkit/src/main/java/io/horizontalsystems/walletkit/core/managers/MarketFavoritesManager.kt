package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.storage.AppDatabase
import io.horizontalsystems.walletkit.core.storage.FavoriteCoin
import io.horizontalsystems.walletkit.core.storage.MarketFavoritesDao
import io.horizontalsystems.walletkit.widgets.MarketWidgetManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class MarketFavoritesManager(
    appDatabase: AppDatabase,
    private val localStorage: ILocalStorage,
    private val marketWidgetManager: MarketWidgetManager
) {
    private val _dataUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val dataUpdatedFlow: Flow<Unit>
        get() = _dataUpdatedFlow

    private val dao: MarketFavoritesDao by lazy {
        appDatabase.marketFavoritesDao()
    }

    fun add(coinUid: String) {
        localStorage.marketFavoritesManualSortingOrder =
            localStorage.marketFavoritesManualSortingOrder.toMutableList().apply {
                add(coinUid)
            }
        dao.insert(FavoriteCoin(coinUid))
        _dataUpdatedFlow.tryEmit(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun addAll(coinUids: List<String>) {
        dao.insertAll(coinUids.map { FavoriteCoin(it) })
        _dataUpdatedFlow.tryEmit(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun remove(coinUid: String) {
        localStorage.marketFavoritesManualSortingOrder =
            localStorage.marketFavoritesManualSortingOrder.toMutableList().apply {
                remove(coinUid)
            }
        dao.delete(coinUid)
        _dataUpdatedFlow.tryEmit(Unit)
        marketWidgetManager.updateWatchListWidgets()
    }

    fun getAll(): List<FavoriteCoin> {
        return dao.getAll()
    }

    fun isCoinInFavorites(coinUid: String): Boolean {
        return dao.getCount(coinUid) > 0
    }
}
