package io.horizontalsystems.bankwallet.modules.guideview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.commonmark.parser.Parser

class GuideViewModel(private val guide: Guide?, private val guidesManager: GuidesManager) : ViewModel() {

    val blocks = MutableLiveData<List<GuideBlock>>()
    private var disposable: Disposable? = null

    init {
        guide?.fileUrl?.let {
            disposable = guidesManager.getGuideContent(it)
                    .subscribeOn(Schedulers.io())
                    .subscribe { content, _ ->
                        didFetchGuideContent(content)
                    }
        }
    }

    private fun didFetchGuideContent(content: String) {
        val parser = Parser.builder().build()
        val document = parser.parse(content)

        val guideVisitor = GuideVisitorBlock()
        document.accept(guideVisitor)

        blocks.postValue(guideVisitor.blocks + GuideBlock.Footer())
    }

    override fun onCleared() {
        disposable?.dispose()
    }
}
