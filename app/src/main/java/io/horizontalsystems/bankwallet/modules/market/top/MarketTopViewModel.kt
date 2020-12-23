package io.horizontalsystems.bankwallet.modules.market.top

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.DataState
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MarketTopViewModel(
        private val service: MarketTopService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val sortingFields: Array<Field> = Field.values()
    val periods by service::periods

    var sortingField: Field = Field.HighestCap
    var period by service::period

    val marketTopViewItemsLiveData = MutableLiveData<List<MarketTopViewItem>>()
    val loadingLiveData = MutableLiveData(false)
    val errorLiveData = MutableLiveData<String?>(null)

    private val disposable = CompositeDisposable()

    init {
        service.marketTopItemsObservable
                .subscribe {
                    syncItems(it)
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun syncItems(dataState: DataState<List<MarketTopItem>>) {
        loadingLiveData.postValue(dataState.loading)
        errorLiveData.postValue(dataState.errorOrNull?.let { convertErrorMessage(it) })

        if (dataState is DataState.Success) {
            val viewItems = dataState.data.map {
                val formattedRate = App.numberFormatter.formatFiat(it.rate, service.currency.symbol, 2, 2)

                MarketTopViewItem(it.coinCode, it.coinName, formattedRate, it.diff)
            }

            marketTopViewItemsLiveData.postValue(viewItems)
        }
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
}

data class MarketTopViewItem(
        val coinCode: String,
        val coinName: String,
        val rate: String,
        val diff: BigDecimal
)