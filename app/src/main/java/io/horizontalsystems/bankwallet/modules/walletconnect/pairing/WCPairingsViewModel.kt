package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.android.Core
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate

class WCPairingsViewModel : ViewModelUiState<WCPairingsUiState>() {

    private val pairings: List<PairingViewItem>
        get() = WCDelegate.getPairings().map { getPairingViewItem(it) }

    override fun createState() = WCPairingsUiState(
        pairings = pairings,
        closeScreen = pairings.isEmpty(),
    )

    private fun getPairingViewItem(pairing: Core.Model.Pairing): PairingViewItem {
        val metaData = pairing.peerAppMetaData

        return PairingViewItem(
            icon = metaData?.icons?.lastOrNull(),
            name = metaData?.name,
            url = metaData?.url,
            topic = pairing.topic
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
