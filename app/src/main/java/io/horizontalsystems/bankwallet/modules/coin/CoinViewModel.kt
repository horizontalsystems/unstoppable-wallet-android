package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class CoinViewModel(
    private val service: CoinService,
    private val clearables: List<Clearable>,
) : ViewModel() {

    val selectedTab = MutableLiveData(CoinModule.Tab.Overview)
    val tabs = CoinModule.Tab.values()

    val fullCoin by service::fullCoin

    val titleLiveData = MutableLiveData(fullCoin.coin.name)
    val isFavoriteLiveData = LiveDataReactiveStreams.fromPublisher(service.isFavorite.toFlowable(BackpressureStrategy.LATEST))

    var notificationIconVisible = service.notificationsAreEnabled
    var notificationIconActive = false

    val alertNotificationUpdated = MutableLiveData<Unit>()
    val showNotificationMenu = SingleLiveEvent<Pair<CoinType, String>>()

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

    private val disposables = CompositeDisposable()

    init {
        updateAlertNotificationIconState()

        service.alertNotificationUpdatedObservable
            .subscribeIO {
                updateAlertNotificationIconState()
            }
            .let {
                disposables.add(it)
            }
    }

    fun onNotificationClick() {
        // todo replace coinType with coinUid
        showNotificationMenu.postValue(Pair(service.fullCoin.platforms.first().coinType, service.fullCoin.coin.name))
    }

    private fun updateAlertNotificationIconState() {
        notificationIconActive = service.hasPriceAlert
        alertNotificationUpdated.postValue(Unit)
    }

}
