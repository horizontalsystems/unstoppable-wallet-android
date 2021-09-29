package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.Platform
import io.reactivex.disposables.Disposable

class CoinPlatformsViewModel(
    private val service: CoinPlatformsService
) {
    val openPlatformsSelectorEvent = SingleLiveEvent<List<PlatformViewItem>>()

    private var currentRequest: CoinPlatformsService.Request? = null
    private val disposable: Disposable

    init {
        disposable = service.requestObservable
            .subscribeIO {
                handle(it)
            }
    }

    private fun handle(request: CoinPlatformsService.Request) {
        currentRequest = request
        val viewItems = request.marketCoin.platforms.map {
            PlatformViewItem(it, request.currentPlatforms.contains(it))
        }
        openPlatformsSelectorEvent.postValue(viewItems)
    }

    fun onSelect(viewItems: List<PlatformViewItem>) {
        currentRequest?.let { currentRequest ->
            service.select(viewItems.map { it.platform }, currentRequest.marketCoin.coin)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.marketCoin.coin)
        }
    }

    data class PlatformViewItem(
        val platform: Platform,
        val selected: Boolean
    )

}
