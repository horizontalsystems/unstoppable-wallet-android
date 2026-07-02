package io.horizontalsystems.core.modules.moneronetwork.addnode

import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.Caution
import io.horizontalsystems.core.core.ViewModelUiState
import io.horizontalsystems.core.core.managers.MoneroNodeManager
import io.horizontalsystems.core.core.providers.Translator
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
