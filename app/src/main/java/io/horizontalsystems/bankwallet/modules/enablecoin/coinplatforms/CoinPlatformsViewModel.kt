package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.platformCoinType
import io.horizontalsystems.bankwallet.entities.platformType
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable

class CoinPlatformsViewModel(
    private val service: CoinPlatformsService
) : ViewModel() {
    val openPlatformsSelectorEvent = SingleLiveEvent<BottomSheetSelectorMultipleDialog.Config>()

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
        val marketCoin = request.marketCoin
        val config = BottomSheetSelectorMultipleDialog.Config(
            platformCoin = null,
            title = Translator.getString(R.string.CoinPlatformsSelector_Title),
            subtitle = marketCoin.coin.name,
            description = Translator.getString(R.string.CoinPlatformsSelector_Description),
            selectedIndexes = request.currentPlatforms.map { marketCoin.platforms.indexOf(it) },
            viewItems = marketCoin.platforms.map { platform ->
                BottomSheetSelectorViewItem(
                    title = platform.coinType.platformType,
                    subtitle = platform.coinType.platformCoinType
                )
            }
        )
        openPlatformsSelectorEvent.postValue(config)
    }

    fun onSelect(indexes: List<Int>) {
        currentRequest?.let { currentRequest ->
            val platforms = currentRequest.marketCoin.platforms
            service.select(indexes.map { platforms[it] }, currentRequest.marketCoin.coin)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.marketCoin.coin)
        }
    }

}
