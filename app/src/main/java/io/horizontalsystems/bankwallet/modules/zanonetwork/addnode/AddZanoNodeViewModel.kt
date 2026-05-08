package io.horizontalsystems.bankwallet.modules.zanonetwork.addnode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import java.net.MalformedURLException
import java.net.URI

class AddZanoNodeViewModel(
    private val nodeManager: ZanoNodeManager
) : ViewModel() {

    private var url = ""
    private var urlCaution: Caution? = null

    var viewState by mutableStateOf(AddZanoNodeViewState())
        private set

    fun onEnterUrl(enteredUrl: String) {
        urlCaution = null
        url = enteredUrl.trim()
        syncState()
    }

    fun onScreenClose() {
        viewState = AddZanoNodeViewState()
    }

    fun onAddClick() {
        val sourceUri: URI
        try {
            sourceUri = URI(url)
            val hasRequiredProtocol = listOf("https", "http").contains(sourceUri.scheme)
            if (!hasRequiredProtocol) throw MalformedURLException()
        } catch (_: Throwable) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Error_InvalidUrl), Caution.Type.Error)
            syncState()
            return
        }

        if (nodeManager.allNodes.any { it.host == url }) {
            urlCaution = Caution(Translator.getString(R.string.AddMoneroNode_Warning_UrlExists), Caution.Type.Warning)
            syncState()
            return
        }

        nodeManager.addZanoNode(url)
        viewState = AddZanoNodeViewState(closeScreen = true)
    }

    private fun syncState() {
        viewState = AddZanoNodeViewState(urlCaution)
    }
}

data class AddZanoNodeViewState(
    val urlCaution: Caution? = null,
    val closeScreen: Boolean = false
)
