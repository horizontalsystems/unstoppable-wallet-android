package io.horizontalsystems.bankwallet.modules.market.category

import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.market.TopMarket
import io.horizontalsystems.marketkit.models.CoinCategory
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await

class MarketCategoryService(
    private val marketCategoryRepository: MarketCategoryRepository,
    private val currencyManager: CurrencyManager,
    private val languageManager: LanguageManager,
    private val favoritesManager: MarketFavoritesManager,
    private val coinCategory: CoinCategory,
    topMarket: TopMarket = TopMarket.Top100,
    sortingField: SortingField = SortingField.HighestCap,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var syncJob: Job? = null

    private var marketItems: List<MarketItem> = listOf()

    val stateObservable: BehaviorSubject<DataState<List<MarketItemWrapper>>> = BehaviorSubject.create()

    var topMarket: TopMarket = topMarket
        private set

    val sortingFields = SortingField.values().toList()
    var sortingField: SortingField = sortingField
        private set

    val coinCategoryName: String get() = coinCategory.name
    val coinCategoryDescription: String get() = coinCategory.description[languageManager.currentLocaleTag]
        ?: coinCategory.description["en"]
        ?: coinCategory.description.keys.firstOrNull()
        ?: ""
    val coinCategoryImageUrl: String get() = coinCategory.imageUrl

    fun setSortingField(sortingField: SortingField) {
        this.sortingField = sortingField
        sync(false)
    }

    private fun sync(forceRefresh: Boolean) {
        syncJob?.cancel()
        syncJob = coroutineScope.launch {
            try {
                marketItems = marketCategoryRepository
                    .get(
                        coinCategory.uid,
                        topMarket.value,
                        sortingField,
                        topMarket.value,
                        currencyManager.baseCurrency,
                        forceRefresh
                    )
                    .await()
                syncItems()
            } catch (e: Throwable) {
                stateObservable.onNext(DataState.Error(e))
            }
        }
    }

    private fun syncItems() {
        val favorites = favoritesManager.getAll().map { it.coinUid }
        val items = marketItems.map { MarketItemWrapper(it, favorites.contains(it.fullCoin.coin.uid)) }
        stateObservable.onNext(DataState.Success(items))
    }

    fun start() {
        coroutineScope.launch {
            favoritesManager.dataUpdatedAsync.asFlow().collect {
                syncItems()
            }
        }

        sync(true)
    }

    fun refresh() {
        sync(true)
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun addFavorite(coinUid: String) {
        favoritesManager.add(coinUid)
    }

    fun removeFavorite(coinUid: String) {
        favoritesManager.remove(coinUid)
    }
}
