package io.horizontalsystems.bankwallet.modules.markdown

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.commonmark.parser.Parser

class MarkdownViewModel(
        private val connectivityManager: ConnectivityManager,
        private val contentProvider: MarkdownModule.IMarkdownContentProvider) : ViewModel() {

    val statusLiveData = MutableLiveData<LoadStatus>()
    val blocks = MutableLiveData<List<MarkdownBlock>>()

    private var disposables = CompositeDisposable()

    private var status: LoadStatus = LoadStatus.Initial
        set(value) {
            field = value

            statusLiveData.postValue(value)
        }

    init {
        loadContent()

        connectivityManager.networkAvailabilitySignal
                .subscribe {
                    if (connectivityManager.isConnected && status is LoadStatus.Failed) {
                        loadContent()
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun onCleared() {
        disposables.dispose()
    }

    private fun didFetchContent(content: String) {
        val parser = Parser.builder().build()
        val document = parser.parse(content)

        val markdownVisitor = MarkdownVisitorBlock(contentProvider.markdownUrl)

        document.accept(markdownVisitor)

        blocks.postValue(markdownVisitor.blocks + MarkdownBlock.Footer())
    }

    private fun loadContent() {
        contentProvider.getContent()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    status = LoadStatus.Loading
                }
                .subscribe({
                    status = LoadStatus.Loaded

                    didFetchContent(it)
                }, {
                    status = LoadStatus.Failed(it)
                })
                .let {
                    disposables.add(it)
                }
    }

}
