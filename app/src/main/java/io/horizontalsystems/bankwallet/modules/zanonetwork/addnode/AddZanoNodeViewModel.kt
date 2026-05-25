package io.horizontalsystems.bankwallet.modules.zanonetwork.addnode

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import java.net.MalformedURLException
import java.net.URI
import javax.inject.Inject

@HiltViewModel
class AddZanoNodeViewModel @Inject constructor(
    private val nodeManager: ZanoNodeManager
) : ViewModelUiState<AddZanoNodeViewState>() {

    private var url = ""
    private var urlCaution: Caution? = null
    private var closeScreen = false

    override fun createState() = AddZanoNodeViewState(
        urlCaution = urlCaution,
        closeScreen = closeScreen,
    )

    fun onEnterUrl(enteredUrl: String) {
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
            val hasRequiredProtocol = scheme == "https" || scheme == "http"
            val hasHost = !sourceUri.host.isNullOrBlank()
            if (!hasRequiredProtocol || !hasHost) throw MalformedURLException()
        } catch (_: Exception) {
            urlCaution = Caution(Translator.getString(R.string.AddZanoNode_Error_InvalidUrl), Caution.Type.Error)
            emitState()
            return
        }

        if (nodeManager.allNodes.any { it.host == url }) {
            urlCaution = Caution(Translator.getString(R.string.AddZanoNode_Warning_UrlExists), Caution.Type.Warning)
            emitState()
            return
        }

        nodeManager.addZanoNode(url)
        closeScreen = true
        emitState()
    }
}

data class AddZanoNodeViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)
