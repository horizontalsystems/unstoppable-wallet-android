package io.horizontalsystems.bankwallet.modules.markdown

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.ViewState
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.commonmark.parser.Parser
import java.net.URL

class MarkdownViewModel(
    private val networkManager: INetworkManager,
    private val contentUrl: String,
    private val connectivityManager: ConnectivityManager,
) : ViewModel() {

    var markdownBlocks by mutableStateOf<List<MarkdownBlock>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    private var disposables = CompositeDisposable()

    init {
        loadContent()

        connectivityManager.networkAvailabilitySignal
            .subscribe {
                if (connectivityManager.isConnected && viewState is ViewState.Error) {
                    retry()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.dispose()
    }

    fun retry() {
        viewState = ViewState.Loading
        loadContent()
    }

    private fun loadContent() {
        getContent()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                markdownBlocks = getMarkdownBlocks(it)
                viewState = ViewState.Success
            }, {
                viewState = ViewState.Error(it)
            })
            .let {
                disposables.add(it)
            }
    }

    private fun getMarkdownBlocks(content: String): List<MarkdownBlock> {
        val parser = Parser.builder().build()
        val document = parser.parse(content)

        val markdownVisitor = MarkdownVisitorBlock(contentUrl)

        document.accept(markdownVisitor)

        return markdownVisitor.blocks + MarkdownBlock.Footer()
    }

    private fun getContent(): Single<String> {
        val url = URL(contentUrl)
        return networkManager.getMarkdown("${url.protocol}://${url.host}", contentUrl)
    }

}
