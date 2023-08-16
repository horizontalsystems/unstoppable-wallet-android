package cash.p.terminal.modules.enablecoin.coinplatforms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.description
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.core.title
import cash.p.terminal.modules.market.ImageSource
import cash.p.terminal.ui.extensions.BottomSheetSelectorMultipleDialog
import cash.p.terminal.ui.extensions.BottomSheetSelectorViewItem
import io.reactivex.disposables.Disposable

class CoinTokensViewModel(
    private val service: CoinTokensService
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
        val blockchain = request.blockchain
        val selectedTokenIndexes = request.enabledTokens.map { request.tokens.indexOf(it) }

        val config = BottomSheetSelectorMultipleDialog.Config(
            icon = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
            title = blockchain.name,
            description = Translator.getString(R.string.AddressFormatSettings_Description),
            selectedIndexes = selectedTokenIndexes,
            allowEmpty = request.allowEmpty,
            viewItems = request.tokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.type.title,
                    subtitle = token.type.description,
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
            service.select(indexes.map { currentRequest.tokens[it] }, currentRequest.blockchain)
        }
    }

    fun onCancelSelect() {
        currentRequest?.let { currentRequest ->
            service.cancel(currentRequest.blockchain)
        }
    }

}
