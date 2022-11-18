package io.horizontalsystems.bankwallet.modules.walletconnect.pairing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.walletconnect.android.Core
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service

class WCPairingsViewModel(private val wc2Service: WC2Service) : ViewModel() {

    private var pairings = listOf<PairingViewItem>()

    var uiState by mutableStateOf(
        WCPairingsUiState(
            pairings = pairings
        )
    )
        private set

    init {
        updatePairings()
    }

    private fun getPairingViewItem(pairing: Core.Model.Pairing): PairingViewItem {
        pairing.topic
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
            pairings = pairings
        )
    }

    fun delete(pairing: PairingViewItem) {
        wc2Service.deletePairing(pairing.topic)

        updatePairings()
    }

    private fun updatePairings() {
        pairings = wc2Service.getPairings()
            .map {
                getPairingViewItem(it)
            }

        emitState()
    }

    fun deleteAll() {
        TODO("Not yet implemented")
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCPairingsViewModel(App.wc2Service) as T
        }
    }
}

data class PairingViewItem(
    val icon: String?,
    val name: String?,
    val url: String?,
    val topic: String
)

data class WCPairingsUiState(val pairings: List<PairingViewItem>)
