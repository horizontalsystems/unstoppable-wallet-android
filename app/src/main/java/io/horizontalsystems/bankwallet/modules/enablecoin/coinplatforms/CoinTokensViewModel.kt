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
        val selectedTokenIndexes = if (request.currentTokens.isEmpty()) listOf(0) else request.currentTokens.map { fullCoin.supportedTokens.indexOf(it) }
        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(fullCoin.coin.iconUrl, fullCoin.iconPlaceholder),
            title = fullCoin.coin.code,
            description = if (fullCoin.supportedTokens.size > 1) Translator.getString(R.string.CoinPlatformsSelector_Description) else null,
            selectedIndexes = selectedTokenIndexes,
            allowEmpty = request.allowEmpty,
            viewItems = fullCoin.supportedTokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.protocolInfo,
                    subtitle = token.typeInfo,
                    copyableString = token.copyableTypeInfo,
                    icon = token.blockchainType.imageUrl
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
