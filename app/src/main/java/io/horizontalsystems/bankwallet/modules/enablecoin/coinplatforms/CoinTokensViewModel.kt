package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.copyableTypeInfo
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.protocolInfo
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.supportedTokens
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.core.typeInfo
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import io.reactivex.disposables.Disposable

class CoinTokensViewModel(
    private val service: CoinTokensService,
    private val accountManager: IAccountManager
) : ViewModel() {

    var showBottomSheetDialog by mutableStateOf(false)
        private set

    var config: BottomSheetSelectorMultipleDialog.Config? = null
        private set
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

        val supportedTokens = fullCoin.supportedTokens.filter { token ->
            accountManager.activeAccount?.type?.let {
                token.blockchainType.supports(it)
            } ?: false
        }

        val selectedTokenIndexes =
            if (request.currentTokens.isEmpty())
                listOf(0)
            else
                request.currentTokens.map { supportedTokens.indexOf(it) }

        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(fullCoin.coin.imageUrl, fullCoin.iconPlaceholder),
            title = fullCoin.coin.code,
            description = if (fullCoin.supportedTokens.size > 1) Translator.getString(R.string.CoinPlatformsSelector_Description) else null,
            selectedIndexes = selectedTokenIndexes,
            allowEmpty = request.allowEmpty,
            viewItems = supportedTokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.protocolInfo,
                    subtitle = token.typeInfo,
                    copyableString = token.copyableTypeInfo,
                    icon = token.blockchainType.imageUrl
                )
            }
        )
        showBottomSheetDialog = true
        this.config = config
    }

    fun bottomSheetDialogShown() {
        showBottomSheetDialog = false
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
