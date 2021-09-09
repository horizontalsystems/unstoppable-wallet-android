package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.disposables.CompositeDisposable

class GuidesViewModel(private val repository: GuidesRepository) : ViewModel() {

    var categories = listOf<GuideCategory>()
    val guides = MutableLiveData<List<Guide>>()
    val loading = MutableLiveData(false)
    val selectedCategory = MutableLiveData<GuideCategory>()
    val error = MutableLiveData<Throwable?>()

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

    fun onSelectCategory(category: GuideCategory) {
        selectedCategory.postValue(category)
        guides.postValue(category.guides)
    }

    override fun onCleared() {
        disposables.dispose()

        repository.clear()
    }

    private fun didFetchGuideCategories(guideCategories: Array<GuideCategory>) {
        categories = guideCategories.toList()
        onSelectCategory(guideCategories.first())
    }

}
