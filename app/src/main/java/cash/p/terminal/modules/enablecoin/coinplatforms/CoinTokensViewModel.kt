package cash.p.terminal.modules.enablecoin.coinplatforms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.copyableTypeInfo
import cash.p.terminal.core.iconPlaceholder
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.protocolInfo
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.core.supportedTokens
import cash.p.terminal.core.supports
import cash.p.terminal.core.typeInfo
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.extensions.BottomSheetSelectorMultipleDialog
import cash.p.terminal.ui.extensions.BottomSheetSelectorViewItem
import io.reactivex.disposables.Disposable

class CoinTokensViewModel(
    private val service: CoinTokensService,
    private val accountType: AccountType
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
            token.blockchainType.supports(accountType)
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
