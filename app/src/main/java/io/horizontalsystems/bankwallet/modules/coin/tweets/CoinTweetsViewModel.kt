package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.reactivex.disposables.CompositeDisposable

class CoinTweetsViewModel(
    private val service: CoinTweetsService,
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val itemsLiveData = MutableLiveData<DataState<List<Tweet>>>()
    val username by service::username

    init {
        service.stateObservable
            .subscribeIO {
                itemsLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}

