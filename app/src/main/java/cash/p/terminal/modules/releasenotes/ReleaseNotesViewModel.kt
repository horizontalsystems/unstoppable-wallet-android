package cash.p.terminal.modules.releasenotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.domain.usecase.GetReleaseNotesUseCase
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.markdown.MarkdownBlock
import cash.p.terminal.modules.markdown.MarkdownVisitorBlock
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser

class ReleaseNotesViewModel(
    private val getReleaseNotesUseCase: GetReleaseNotesUseCase,
) : ViewModel() {

    private val connectivityManager: ConnectivityManager = App.connectivityManager
    private val appConfigProvider: AppConfigProvider = App.appConfigProvider
    private val releaseNotesManager: ReleaseNotesManager = App.releaseNotesManager

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

    fun whatsNewShown() {
        releaseNotesManager.updateShownAppVersion()
    }

    private fun loadContent() {
        viewModelScope.launch {
            try {
                val content = getReleaseNotesUseCase()
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
}
