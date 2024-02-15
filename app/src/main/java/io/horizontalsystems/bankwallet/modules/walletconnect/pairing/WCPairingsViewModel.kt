package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.android.Core
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate

class WCPairingsViewModel : ViewModel() {

    private val pairings: List<PairingViewItem>
        get() = WCDelegate.getPairings().map { getPairingViewItem(it) }

    var uiState by mutableStateOf(
        WCPairingsUiState(
            pairings = pairings,
            closeScreen = pairings.isEmpty(),
        )
    )
        private set

    private fun getPairingViewItem(pairing: Core.Model.Pairing): PairingViewItem {
        val metaData = pairing.peerAppMetaData

        return PairingViewItem(
            icon = metaData?.icons?.lastOrNull(),
            name = metaData?.name,
            url = metaData?.url,
            topic = pairing.topic
        )
    }

    private fun emitState() {
        uiState = WCPairingsUiState(
            pairings = pairings,
            closeScreen = pairings.isEmpty()
        )
    }

    fun delete(pairing: PairingViewItem) {
        WCDelegate.deletePairing(topic = pairing.topic)
        emitState()
    }

    fun deleteAll() {
        WCDelegate.deleteAllPairings()
        emitState()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCPairingsViewModel() as T
        }
    }
}

data class PairingViewItem(
    val icon: String?,
    val name: String?,
    val url: String?,
    val topic: String
)

data class WCPairingsUiState(
    val pairings: List<PairingViewItem>,
    val closeScreen: Boolean
)
