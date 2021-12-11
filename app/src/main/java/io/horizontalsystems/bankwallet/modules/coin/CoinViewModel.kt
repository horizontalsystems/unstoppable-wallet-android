package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
) : ViewModel() {

    val selectedTab = MutableLiveData(CoinModule.Tab.Overview)
    val tabs = CoinModule.Tab.values()

    val fullCoin by service::fullCoin

    val titleLiveData = MutableLiveData(fullCoin.coin.code)
    val isFavoriteLiveData = LiveDataReactiveStreams.fromPublisher(service.isFavorite.toFlowable(BackpressureStrategy.LATEST))
    val coinStateLiveData = MutableLiveData<CoinState>()

    val successMessageLiveEvent = SingleLiveEvent<Int>()
    val warningMessageLiveEvent = SingleLiveEvent<Int>()

    private val disposables = CompositeDisposable()

    init {
        service.coinState
            .subscribeIO {
                coinStateLiveData.postValue(it)
                if (it == CoinState.AddedToWallet) {
                    successMessageLiveEvent.postValue(R.string.Hud_Added_To_Wallet)
                }
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
        clearables.forEach(Clearable::clear)
    }

    fun onSelect(tab: CoinModule.Tab) {
        selectedTab.postValue(tab)
    }

    fun onFavoriteClick() {
        service.favorite()
        successMessageLiveEvent.postValue(R.string.Hud_Added_To_Watchlist)
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
        successMessageLiveEvent.postValue(R.string.Hud_Removed_from_Watchlist)
    }

    fun onClickInWallet() {
        warningMessageLiveEvent.postValue(R.string.Hud_Already_In_Wallet)
    }

}
