package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.reactivex.BackpressureStrategy

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
) : ViewModel() {

    val selectedTab = MutableLiveData(CoinModule.Tab.Overview)
    val tabs = CoinModule.Tab.values()

    val fullCoin by service::fullCoin

    val titleLiveData = MutableLiveData(fullCoin.coin.code)
    val isFavoriteLiveData = LiveDataReactiveStreams.fromPublisher(service.isFavorite.toFlowable(BackpressureStrategy.LATEST))

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun onSelect(tab: CoinModule.Tab) {
        selectedTab.postValue(tab)
    }

    fun onFavoriteClick() {
        service.favorite()
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
    }

}
