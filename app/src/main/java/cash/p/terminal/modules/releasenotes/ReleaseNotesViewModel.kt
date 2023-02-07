package cash.p.terminal.modules.releasenotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.INetworkManager
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.markdown.MarkdownBlock
import cash.p.terminal.modules.markdown.MarkdownVisitorBlock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser
import java.net.URL

class ReleaseNotesViewModel(
    private val networkManager: INetworkManager,
    private val contentUrl: String,
    private val connectivityManager: ConnectivityManager,
    appConfigProvider: AppConfigProvider
) : ViewModel() {

    val twitterUrl = appConfigProvider.appTwitterLink
    val telegramUrl = appConfigProvider.appTelegramLink
    val redditUrl = appConfigProvider.appRedditLink

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

        return markdownVisitor.blocks + MarkdownBlock.Footer()
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
