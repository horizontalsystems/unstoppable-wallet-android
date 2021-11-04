package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule
import io.reactivex.disposables.CompositeDisposable

class CoinTweetsViewModel(
    private val service: CoinTweetsService,
) : ViewModel() {
    val username by service::username

    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val itemsLiveData = MutableLiveData<List<Tweet>>()
    val viewStateLiveData = MutableLiveData<TvlModule.ViewState>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO { state ->
                isRefreshingLiveData.postValue(state == DataState.Loading)

                state.dataOrNull?.let {
                    itemsLiveData.postValue(it)
                }

                state.viewState?.let {
                    viewStateLiveData.postValue(it)
                }

            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    val DataState<*>.viewState: TvlModule.ViewState?
        get() = when (this) {
            is DataState.Error -> TvlModule.ViewState.Error
            is DataState.Success -> TvlModule.ViewState.Success
            else -> null
        }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}

