package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable

class CoinTokensViewModel(
    private val service: CoinTokensService
) : ViewModel() {
    val openSelectorEvent = SingleLiveEvent<BottomSheetSelectorMultipleDialog.Config>()

    private var currentRequest: CoinTokensService.Request? = null
    private val disposable: Disposable

    init {
        disposable = service.requestObservable
            .subscribeIO {
                handle(it)
            }
    }

    private fun handle(request: CoinTokensService.Request) {
        currentRequest = request
        val fullCoin = request.fullCoin
        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(fullCoin.coin.iconUrl, fullCoin.iconPlaceholder),
            title = fullCoin.coin.name,
            descriptionTitle = Translator.getString(R.string.CoinPlatformsSelector_Title),
            description = Translator.getString(R.string.CoinPlatformsSelector_Description),
            selectedIndexes = request.currentTokens.map { fullCoin.supportedTokens.indexOf(it) },
            viewItems = fullCoin.supportedTokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.protocolType ?: "",
                    subtitle = token.protocolInfo
                )
            }
        )
        openSelectorEvent.postValue(config)
    }

    fun onSelect(indexes: List<Int>) {
        currentRequest?.let { currentRequest ->
            val platforms = currentRequest.fullCoin.supportedTokens
            service.select(indexes.map { platforms[it] }, currentRequest.fullCoin.coin)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.fullCoin)
        }
    }

}
