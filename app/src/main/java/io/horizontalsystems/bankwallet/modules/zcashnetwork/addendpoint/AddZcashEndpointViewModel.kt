package io.horizontalsystems.bankwallet.modules.zcashnetwork.addendpoint

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import java.net.MalformedURLException
import java.net.URI

class AddZcashEndpointViewModel(
    private val endpointManager: ZcashLightWalletEndpointManager
) : ViewModelUiState<AddZcashEndpointViewState>() {

    private var url = ""
    private var urlCaution: Caution? = null
    private var closeScreen = false

    override fun createState() = AddZcashEndpointViewState(
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
            urlCaution = Caution(Translator.getString(R.string.AddZcashEndpoint_Error_InvalidUrl), Caution.Type.Error)
            emitState()
            return
        }

        if (endpointManager.allEndpoints.any { it.url == url }) {
            urlCaution = Caution(Translator.getString(R.string.AddZcashEndpoint_Warning_UrlExists), Caution.Type.Warning)
            emitState()
            return
        }

        endpointManager.addCustomEndpoint(url)
        closeScreen = true
        emitState()
    }
}

data class AddZcashEndpointViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)
