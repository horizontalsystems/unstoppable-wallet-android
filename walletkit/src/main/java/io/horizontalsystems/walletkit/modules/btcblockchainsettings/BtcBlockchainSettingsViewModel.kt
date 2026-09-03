package io.horizontalsystems.walletkit.modules.btcblockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.walletkit.modules.btcblockchainsettings.BtcBlockchainSettingsModule.BlockchainSettingsIcon
import io.horizontalsystems.walletkit.modules.btcblockchainsettings.BtcBlockchainSettingsModule.ViewItem
import kotlinx.coroutines.launch

class BtcBlockchainSettingsViewModel(
    private val service: BtcBlockchainSettingsService
) : ViewModel() {

    var closeScreen by mutableStateOf(false)
        private set

    var restoreSources by mutableStateOf<List<ViewItem>>(listOf())
        private set

    var saveButtonEnabled by mutableStateOf(false)
        private set

    val title: String = service.blockchain.name
    val blockchainIconUrl = service.blockchain.type.imageUrl

    init {
        viewModelScope.launch {
            service.hasChangesFlow.collect {
                saveButtonEnabled = it
                syncRestoreModeState()
            }
        }

        syncRestoreModeState()
    }

    fun onSelectRestoreMode(viewItem: ViewItem) {
        service.setRestoreMode(viewItem.id)
    }

    fun onSaveClick() {
        service.save()
        closeScreen = true
    }

    private fun syncRestoreModeState() {
        val viewItems = service.restoreModes.map { mode ->
            ViewItem(
                id = mode.raw,
                title = Translator.getString(mode.title),
                subtitle = Translator.getString(mode.description),
                selected = mode == service.restoreMode,
                icon = mode.icon
            )
        }
        restoreSources = viewItems
    }

    private val BtcRestoreMode.icon: BlockchainSettingsIcon
        get() = when (this) {
            BtcRestoreMode.Blockchair -> BlockchainSettingsIcon.ApiIcon(R.drawable.ic_blockchair)
            BtcRestoreMode.Hybrid -> BlockchainSettingsIcon.ApiIcon(R.drawable.ic_api_hybrid)
            BtcRestoreMode.Blockchain -> BlockchainSettingsIcon.BlockchainIcon(service.blockchain.type.imageUrl)
        }

}
