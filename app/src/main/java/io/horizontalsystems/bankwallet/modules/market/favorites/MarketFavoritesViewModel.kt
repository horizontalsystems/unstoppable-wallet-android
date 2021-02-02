package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.modules.market.top.MarketField
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopItem
import io.horizontalsystems.bankwallet.modules.market.top.Score
import io.horizontalsystems.bankwallet.modules.market.top.SortingField
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.*

class MarketFavoritesViewModel(
        private val service: MarketFavoritesService,
        private val connectivityManager: ConnectivityManager,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<SortingField> by service::sortingFields

    var sortingField: SortingField = sortingFields.first()
        private set

    var marketField: MarketField = MarketField.PriceDiff
        private set

    fun update(sortingField: SortingField? = null, marketField: MarketField? = null) {
        sortingField?.let {
            this.sortingField = it
        }
        marketField?.let {
            this.marketField = it
        }
        syncViewItemsBySortingField()
    }

    val marketTopViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val networkNotAvailable = SingleLiveEvent<Unit>()

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketFavoritesService.State) {
        loadingLiveData.postValue(state is MarketFavoritesService.State.Loading)

        if (state is MarketFavoritesService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketFavoritesService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketFavoritesService.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun syncViewItemsBySortingField() {
        val viewItems = sort(service.marketTopItems, sortingField).map {
            val formattedRate = App.numberFormatter.formatFiat(it.rate, service.currency.symbol, 2, 2)
            val marketDataValue = when (marketField) {
                MarketField.MarketCap -> {
                    val marketCapFormatted = it.marketCap?.let { marketCap ->
                        val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketCap)
                        App.numberFormatter.formatFiat(shortenValue, service.currency.symbol, 0, 2) + suffix
                    }

                    MarketTopViewItem.MarketDataValue.MarketCap(marketCapFormatted ?: App.instance.getString(R.string.NotAvailable))
                }
                MarketField.Volume -> {
                    val (shortenValue, suffix) = App.numberFormatter.shortenValue(it.volume)
                    val volumeFormatted = App.numberFormatter.formatFiat(shortenValue, service.currency.symbol, 0, 2) + suffix

                    MarketTopViewItem.MarketDataValue.Volume(volumeFormatted)
                }
                MarketField.PriceDiff -> MarketTopViewItem.MarketDataValue.Diff(it.diff)
            }
            MarketTopViewItem(it.score, it.coinCode, it.coinName, formattedRate, it.diff, marketDataValue)
        }

        marketTopViewItemsLiveData.postValue(viewItems)
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }


    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposable.clear()
        super.onCleared()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

    private fun sort(items: List<MarketTopItem>, sortingField: SortingField) = when (sortingField) {
        SortingField.HighestCap -> items.sortedByDescendingNullLast { it.marketCap }
        SortingField.LowestCap -> items.sortedByNullLast { it.marketCap }
        SortingField.HighestVolume -> items.sortedByDescendingNullLast { it.volume }
        SortingField.LowestVolume -> items.sortedByNullLast { it.volume }
        SortingField.HighestPrice -> items.sortedByDescendingNullLast { it.rate }
        SortingField.LowestPrice -> items.sortedByNullLast { it.rate }
        SortingField.TopGainers -> items.sortedByDescendingNullLast { it.diff }
        SortingField.TopLosers -> items.sortedByNullLast { it.diff }
    }

}

data class MarketTopViewItem(
        val score: Score?,
        val coinCode: String,
        val coinName: String,
        val rate: String,
        val diff: BigDecimal,
        val marketDataValue: MarketDataValue
) {
    sealed class MarketDataValue {
        class MarketCap(val value: String) : MarketDataValue()
        class Volume(val value: String) : MarketDataValue()
        class Diff(val value: BigDecimal) : MarketDataValue()
    }

    fun areItemsTheSame(other: MarketTopViewItem): Boolean {
        return coinCode == other.coinCode && coinName == other.coinName
    }

    fun areContentsTheSame(other: MarketTopViewItem): Boolean {
        return this == other
    }
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescendingNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareByDescending(selector)))
}

inline fun <T, R : Comparable<R>> Iterable<T>.sortedByNullLast(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(Comparator.nullsLast(compareBy(selector)))
}
