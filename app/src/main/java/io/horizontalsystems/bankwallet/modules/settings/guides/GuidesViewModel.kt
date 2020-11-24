package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.disposables.CompositeDisposable

class GuidesViewModel(val repository: GuidesRepository) : ViewModel() {

    val guides = MutableLiveData<List<Guide>>()
    val loading = MutableLiveData<Boolean>(false)
    val filters = MutableLiveData<List<String>>()
    val error = MutableLiveData<Throwable?>()

    private var guideCategories: Array<GuideCategory> = arrayOf()
    private var currentCategoryIndex = 0
    private var disposables = CompositeDisposable()

    init {
        repository.guideCategories
                .subscribe {
                    loading.postValue(it is DataState.Loading)

                    if (it is DataState.Success) {
                        didFetchGuideCategories(it.data)
                    }

                    error.postValue((it as? DataState.Error)?.throwable)
                }
                .let {
                    disposables.add(it)
                }
    }

    fun onSelectFilter(filterId: String) {
        currentCategoryIndex = guideCategories.indexOfFirst {
            it.category == filterId
        }

        syncViewItems()
    }

    override fun onCleared() {
        disposables.dispose()

        repository.clear()
    }

    private fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        this.guideCategories = guideCategories

        filters.postValue(guideCategories.map { it.category })

        syncViewItems()
    }

    private fun syncViewItems() {
        guides.postValue(guideCategories[currentCategoryIndex].guides)
    }
}
