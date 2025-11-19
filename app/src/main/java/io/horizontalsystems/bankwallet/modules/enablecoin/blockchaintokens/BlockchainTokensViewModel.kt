package io.horizontalsystems.bankwallet.modules.enablecoin.blockchaintokens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorViewItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BlockchainTokensViewModel(
    private val service: BlockchainTokensService
) : ViewModel() {

    var showBottomSheetDialog by mutableStateOf(false)
        private set

    var config: BottomSheetSelectorMultipleDialog.Config? = null
        private set
    private var currentRequest: BlockchainTokensService.Request? = null

    init {
        viewModelScope.launch {
            service.requestObservable.asFlow().collect {
                handle(it)
            }
        }
    }

    private fun handle(request: BlockchainTokensService.Request) {
        currentRequest = request
        val blockchain = request.blockchain
        val selectedTokenIndexes = request.enabledTokens.map { request.tokens.indexOf(it) }

        val config = BottomSheetSelectorMultipleDialog.Config(
            title = blockchain.name,
            selectedIndexes = selectedTokenIndexes,
            allowEmpty = request.allowEmpty,
            viewItems = request.tokens.map { token ->
                BottomSheetSelectorViewItem(
                    title = token.type.description,
                    subtitle = token.type.title,
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
