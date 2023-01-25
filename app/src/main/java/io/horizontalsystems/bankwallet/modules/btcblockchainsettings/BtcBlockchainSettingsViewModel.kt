package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bankwallet.modules.btcblockchainsettings.BtcBlockchainSettingsModule.ViewItem
import io.reactivex.disposables.CompositeDisposable

class BtcBlockchainSettingsViewModel(
    private val service: BtcBlockchainSettingsService
) : ViewModel() {

    private val disposables = CompositeDisposable()

    var closeScreen by mutableStateOf(false)
        private set

    var restoreSources by mutableStateOf<List<ViewItem>>(listOf())
        private set

    var transactionSortModes by mutableStateOf<List<ViewItem>>(listOf())
        private set

    var saveButtonEnabled by mutableStateOf(false)
        private set

    val title: String = service.blockchain.name
    val blockchainIconUrl = service.blockchain.type.imageUrl

    init {
        service.hasChangesObservable
            .subscribe {
                saveButtonEnabled = it
                syncRestoreModeState()
                syncTransactionModeState()
            }.let {
                disposables.add(it)
            }

        syncRestoreModeState()
        syncTransactionModeState()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onSelectRestoreMode(viewItem: ViewItem) {
        service.setRestoreMode(viewItem.id)
    }

    fun onSelectTransactionMode(viewItem: ViewItem) {
        service.setTransactionMode(viewItem.id)
    }

    fun onSaveClick() {
        service.save()
        closeScreen = true
    }

    private fun syncRestoreModeState() {
        val viewItems = BtcRestoreMode.values().map { mode ->
            ViewItem(
                id = mode.raw,
                title = Translator.getString(mode.title),
                subtitle = Translator.getString(mode.description),
                selected = mode == service.restoreMode
            )
        }
        restoreSources = viewItems
    }

    private fun syncTransactionModeState() {
        val viewItems = TransactionDataSortMode.values().map { mode ->
            ViewItem(
                id = mode.raw,
                title = Translator.getString(mode.title),
                subtitle = Translator.getString(mode.description),
                selected = mode == service.transactionMode
            )
        }
        transactionSortModes = viewItems
    }

}
