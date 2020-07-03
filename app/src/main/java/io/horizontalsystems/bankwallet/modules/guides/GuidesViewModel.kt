package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class GuidesViewModel(private val guidesManager: GuidesManager, private val connectivityManager: ConnectivityManager) : ViewModel() {

    val openGuide = SingleLiveEvent<Guide>()
    val guidesLiveData = MutableLiveData<List<Guide>>()
    val statusLiveData = MutableLiveData<LoadStatus>()
    val filters = MutableLiveData<List<String>>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0
    private var disposables = CompositeDisposable()

    private var status: LoadStatus = LoadStatus.Initial
        set(value) {
            field = value

            statusLiveData.postValue(value)
        }

    init {
        loadGuidesList()

        connectivityManager.networkAvailabilitySignal
                .subscribe {
                    if (connectivityManager.isConnected && status is LoadStatus.Failed) {
                        loadGuidesList()
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    fun onSelectFilter(filterId: String) {
        currentCategoryIndex = guideCategories.indexOfFirst {
            it.title == filterId
        }

        syncViewItems()
    }

    fun onGuideClick(guide: Guide) {
        openGuide.postValue(guide)
    }

    override fun onCleared() {
        disposables.dispose()
    }

    private fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.title })

        syncViewItems()
    }

    private fun syncViewItems() {
        guidesLiveData.postValue(guideCategories[currentCategoryIndex].guides)
    }

    private fun loadGuidesList() {
        guidesManager.getGuideCategories()
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    status = LoadStatus.Loading
                }
                .subscribe({
                    status = LoadStatus.Loaded

                    didFetchGuideCategories(it)
                }, {
                    status = LoadStatus.Failed(it)
                })
                .let {
                    disposables.add(it)
                }
    }
}
