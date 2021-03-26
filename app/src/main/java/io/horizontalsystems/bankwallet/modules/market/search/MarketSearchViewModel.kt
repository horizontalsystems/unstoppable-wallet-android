package io.horizontalsystems.bankwallet.modules.market.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class MarketSearchViewModel(private val service: MarketSearchService, private val clearables: List<Clearable>) : ViewModel() {

    private val disposable = CompositeDisposable()
    private val querySubject = PublishSubject.create<String>()

    var query: String = ""
        set(value) {
            field = value
            querySubject.onNext(value)
        }

    val itemsLiveData = MutableLiveData<List<CoinDataViewItem>>()
    val emptyResultsLiveData = MutableLiveData(false)
    val advancedSearchButtonVisibleLiveDataViewItem = MutableLiveData(service.query.isBlank())

    init {
        service.itemsAsync
                .subscribeIO {
                    emptyResultsLiveData.postValue(it.isPresent && it.get().isEmpty())
                    advancedSearchButtonVisibleLiveDataViewItem.postValue(service.query.isBlank())

                    itemsLiveData.postValue(it.orElse(listOf()).map { CoinDataViewItem(it.code, it.title, it.type) })
                }.let {
                    disposable.add(it)
                }

        querySubject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribe {
                    service.query = it
                }.let {
                    disposable.add(it)
                }
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

}
