package io.horizontalsystems.bankwallet.modules.guideview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.modules.guides.LoadStatus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.commonmark.parser.Parser

class GuideViewModel(private val guide: Guide?, private val guidesManager: GuidesManager, private val connectivityManager: ConnectivityManager) : ViewModel() {

    val statusLiveData = MutableLiveData<LoadStatus>()
    val blocks = MutableLiveData<List<GuideBlock>>()

    private var disposables = CompositeDisposable()

    private var status: LoadStatus = LoadStatus.Initial
        set(value) {
            field = value

            statusLiveData.postValue(value)
        }

    init {
        loadGuide()

        connectivityManager.networkAvailabilitySignal
                .subscribe {
                    if (connectivityManager.isConnected && status is LoadStatus.Failed) {
                        loadGuide()
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun onCleared() {
        disposables.dispose()
    }

    private fun didFetchGuideContent(content: String) {
        val parser = Parser.builder().build()
        val document = parser.parse(content)

        val guideVisitor = GuideVisitorBlock()
        document.accept(guideVisitor)

        blocks.postValue(guideVisitor.blocks + GuideBlock.Footer())
    }

    private fun loadGuide() {
        guide?.fileUrl?.let {
            guidesManager.getGuideContent(it)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe {
                        status = LoadStatus.Loading
                    }
                    .subscribe({
                        status = LoadStatus.Loaded

                        didFetchGuideContent(it)
                    }, {
                        status = LoadStatus.Failed(it)
                    })
                    .let {
                        disposables.add(it)
                    }
        }
    }
}
