package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.*

class MarketTopViewModel(
        private val service: MarketTopService,
        private val connectivityManager: ConnectivityManager,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<Field> by service::sortingFields

    var sortingField: Field = sortingFields.first()
        set(value) {
            field = value

            syncViewItemsBySortingField()
        }
    var additionalField: AdditionalField = AdditionalField.MarketCap
        set(value) {
            field = value

            syncViewItemsBySortingField()
        }

    val marketTopViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)
    val networkNotAvailable = SingleLiveEvent<Unit>()

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribe {
                    syncState(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncState(state: MarketTopService.State) {
        loadingLiveData.postValue(state is MarketTopService.State.Loading)

        if (state is MarketTopService.State.Error && !connectivityManager.isConnected) {
            networkNotAvailable.postValue(Unit)
        }

        errorLiveData.postValue((state as? MarketTopService.State.Error)?.error?.let { convertErrorMessage(it) })

        if (state is MarketTopService.State.Loaded) {
            syncViewItemsBySortingField()
        }
    }

    private fun syncViewItemsBySortingField() {
        val viewItems = sort(service.marketTopItems, sortingField).map {
            val formattedRate = App.numberFormatter.formatFiat(it.rate, service.currency.symbol, 2, 2)
            val xxx = when (additionalField) {
                AdditionalField.MarketCap -> {
                    val marketCapFormatted = it.marketCap?.let { marketCap ->
                        val (shortenValue, suffix) = App.numberFormatter.shortenValue(marketCap)
                        App.numberFormatter.formatFiat(shortenValue, service.currency.symbol, 0, 2) + suffix
                    }

                    MarketTopViewItem.Xxx.MarketCap(marketCapFormatted ?: App.instance.getString(R.string.NotAvailable))
                }
                AdditionalField.Volume -> {
                    val (shortenValue, suffix) = App.numberFormatter.shortenValue(it.volume)
                    val volumeFormatted = App.numberFormatter.formatFiat(shortenValue, service.currency.symbol, 0, 2) + suffix

                    MarketTopViewItem.Xxx.Volume(volumeFormatted)
                }
                AdditionalField.PriceDiff -> MarketTopViewItem.Xxx.Diff(it.diff)
            }
            MarketTopViewItem(it.rank, it.coinCode, it.coinName, formattedRate, it.diff, xxx)
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

    private fun sort(items: List<MarketTopItem>, sortingField: Field) = when (sortingField) {
        Field.HighestCap -> items.sortedByDescendingNullLast { it.marketCap }
        Field.LowestCap -> items.sortedByNullLast { it.marketCap }
        Field.HighestVolume -> items.sortedByDescendingNullLast { it.volume }
        Field.LowestVolume -> items.sortedByNullLast { it.volume }
        Field.HighestPrice -> items.sortedByDescendingNullLast { it.rate }
        Field.LowestPrice -> items.sortedByNullLast { it.rate }
        Field.TopGainers -> items.sortedByDescendingNullLast { it.diff }
        Field.TopLosers -> items.sortedByNullLast { it.diff }
    }

}

data class MarketTopViewItem(
        val rank: Int,
        val coinCode: String,
        val coinName: String,
        val rate: String,
        val diff: BigDecimal,
        val xxx: Xxx
) {
    sealed class Xxx {
        class MarketCap(val value: String) : Xxx()
        class Volume(val value: String) : Xxx()
        class Diff(val value: BigDecimal) : Xxx()
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
