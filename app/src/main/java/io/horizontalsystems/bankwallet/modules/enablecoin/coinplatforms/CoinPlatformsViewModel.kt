package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.platformCoinType
import io.horizontalsystems.bankwallet.entities.platformType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
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
        val fullCoin = request.fullCoin
        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(fullCoin.coin.iconUrl, fullCoin.iconPlaceholder),
            title = Translator.getString(R.string.CoinPlatformsSelector_Title),
            subtitle = fullCoin.coin.name,
            description = Translator.getString(R.string.CoinPlatformsSelector_Description),
            selectedIndexes = request.currentPlatforms.map { fullCoin.platforms.indexOf(it) },
            viewItems = fullCoin.platforms.map { platform ->
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
            val platforms = currentRequest.fullCoin.platforms
            service.select(indexes.map { platforms[it] }, currentRequest.fullCoin.coin)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.fullCoin.coin)
        }
    }

}
