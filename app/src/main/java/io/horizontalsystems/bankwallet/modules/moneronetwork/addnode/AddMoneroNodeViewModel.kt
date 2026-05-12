package io.horizontalsystems.bankwallet.modules.moneronetwork.addnode

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.marketkit.models.BlockchainType
import java.net.MalformedURLException
import java.net.URI

class AddMoneroNodeViewModel(
    private val nodeManager: MoneroNodeManager
) : ViewModelUiState<AddMoneroNodeViewState>() {

    private var url = ""
    private var username: String? = null
    private var password: String? = null
    private var urlCaution: Caution? = null
    private var closeScreen = false

    override fun createState() = AddMoneroNodeViewState(
        urlCaution = urlCaution,
        closeScreen = closeScreen,
    )

    fun onEnterUsername(username: String) {
        this.username = username.trim()
    }

    fun onEnterPassword(password: String) {
        this.password = password
    }

    fun onEnterRpcUrl(enteredUrl: String) {
        urlCaution = null
        url = enteredUrl.trim()
        emitState()
    }

    fun onScreenClose() {
        urlCaution = null
        closeScreen = false
        emitState()
    }

    fun onAddClick() {
        val sourceUri: URI

        try {
            sourceUri = URI(url)
            val scheme = sourceUri.scheme?.lowercase()
            val hasRequiredProtocol = scheme == "https"
            val hasHost = !sourceUri.host.isNullOrBlank()
            if (!hasRequiredProtocol || !hasHost) throw MalformedURLException()
        } catch (_: Exception) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Error_InvalidUrl), Caution.Type.Error)
            emitState()
            return
        }

        if (nodeManager.allNodes.any { it.host == url }) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Warning_UrlExists), Caution.Type.Warning)
            emitState()
            return
        }

        nodeManager.addMoneroNode(url, username, password, true)

        closeScreen = true
        emitState()
    }
}

data class AddMoneroNodeViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)
