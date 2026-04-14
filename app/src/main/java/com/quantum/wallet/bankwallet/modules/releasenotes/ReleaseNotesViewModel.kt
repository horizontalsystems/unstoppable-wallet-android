package com.quantum.wallet.bankwallet.modules.releasenotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.INetworkManager
import com.quantum.wallet.bankwallet.core.managers.ConnectivityManager
import com.quantum.wallet.bankwallet.core.managers.ReleaseNotesManager
import com.quantum.wallet.bankwallet.core.providers.AppConfigProvider
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.markdown.MarkdownBlock
import com.quantum.wallet.bankwallet.modules.markdown.MarkdownVisitorBlock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import java.net.URL
import java.util.Calendar

class ReleaseNotesViewModel(
    private val networkManager: INetworkManager,
    private val contentUrl: String,
    private val connectivityManager: ConnectivityManager,
    private val releaseNotesManager: ReleaseNotesManager,
    appConfigProvider: AppConfigProvider
) : ViewModel() {

    val twitterUrl = appConfigProvider.appTwitterLink
    val telegramUrl = appConfigProvider.appTelegramLink

    var markdownBlocks by mutableStateOf<List<MarkdownBlock>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    init {
        loadContent()

        connectivityManager.networkAvailabilityFlow
            .onEach {
                if (connectivityManager.isConnected && viewState is ViewState.Error) {
                    retry()
                }
            }
            .launchIn(viewModelScope)
    }

    fun retry() {
        viewState = ViewState.Loading
        loadContent()
    }

    fun whatsNewShown() {
        releaseNotesManager.updateShownAppVersion()
    }

    private fun loadContent() {
        viewModelScope.launch {
            try {
                val content = getContent()
                markdownBlocks = getMarkdownBlocks(content)
                viewState = ViewState.Success
            } catch (e: Exception) {
                viewState = ViewState.Error(e)
            }
        }
    }

    private fun getMarkdownBlocks(content: String): List<MarkdownBlock> {
        val parser = Parser.builder().build()
        val document = parser.parse(content)

        val markdownVisitor = MarkdownVisitorBlock()

        document.accept(markdownVisitor)

        val footerText = getFooterText()

        return markdownVisitor.blocks + MarkdownBlock.Footer(footerText)
    }

    private fun getFooterText(): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val footerText = Translator.getString(R.string.FooterText, currentYear)
        return footerText
    }

    private suspend fun getContent(): String {
        val url = URL(contentUrl)
        val releaseNotesJsonObject =
            networkManager.getReleaseNotes("${url.protocol}://${url.host}", contentUrl)

        return when {
            releaseNotesJsonObject.has("body") -> releaseNotesJsonObject.asJsonObject["body"].asString
            else -> ""
        }
    }
}
