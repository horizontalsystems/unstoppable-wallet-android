package cash.p.terminal.modules.markdown.localreader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.modules.markdown.MarkdownBlock
import cash.p.terminal.modules.markdown.MarkdownVisitorBlock
import cash.p.terminal.ui_compose.entities.ViewState
import kotlinx.coroutines.launch
import org.commonmark.parser.Parser

class MarkdownLocalViewModel : ViewModel() {

    var markdownBlocks by mutableStateOf<List<MarkdownBlock>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    fun parseContent(markdownContent: String) {
        viewModelScope.launch {
            try {
                markdownBlocks = getMarkdownBlocks(markdownContent)
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
