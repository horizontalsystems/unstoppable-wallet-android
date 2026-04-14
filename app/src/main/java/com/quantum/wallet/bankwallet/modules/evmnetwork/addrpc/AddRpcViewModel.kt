package com.quantum.wallet.bankwallet.modules.evmnetwork.addrpc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.Caution
import com.quantum.wallet.bankwallet.core.managers.EvmSyncSourceManager
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import io.horizontalsystems.marketkit.models.Blockchain
import java.net.MalformedURLException
import java.net.URI

class AddRpcViewModel(
    private val blockchain: Blockchain,
    private val evmSyncSourceManager: EvmSyncSourceManager
) : ViewModel() {

    private var url = ""
    private var auth: String? = null
    private var urlCaution: Caution? = null

    var viewState by mutableStateOf(AddRpcViewState(null))
        private set

    fun onEnterBasicAuth(basicAuth: String) {
        auth = basicAuth.trim()
    }

    fun onEnterRpcUrl(enteredUrl: String) {
        urlCaution = null
        url = enteredUrl.trim()
        syncState()
    }

    fun onScreenClose() {
        viewState = AddRpcViewState()
    }

    fun onAddClick() {
        val sourceUri: URI

        try {
            sourceUri = URI(url)
            val hasRequiredProtocol = listOf("https", "wss").contains(sourceUri.scheme)
            if (!hasRequiredProtocol) {
                throw MalformedURLException()
            }
        } catch (e: Throwable) {
            urlCaution = Caution(Translator.getString(R.string.AddEvmSyncSource_Error_InvalidUrl), Caution.Type.Error)
            syncState()
            return
        }

        val existingSources = evmSyncSourceManager.allSyncSources(blockchain.type)

        if (existingSources.any { it.uri == sourceUri}) {
            urlCaution = Caution(Translator.getString(R.string.AddEvmSyncSource_Warning_UrlExists), Caution.Type.Warning)
            syncState()
            return
        }

        evmSyncSourceManager.saveSyncSource(blockchain.type, url, auth)

        viewState = AddRpcViewState(null, true)

        stat(
            page = StatPage.BlockchainSettingsEvmAdd,
            event = StatEvent.AddEvmSource(blockchain.uid)
        )
    }

    private fun syncState() {
        viewState = AddRpcViewState(urlCaution)
    }
}

data class AddRpcViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)