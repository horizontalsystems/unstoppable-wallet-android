package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class GuidesViewModel(private val repository: GuidesRepository) : ViewModel() {

    var categories by mutableStateOf<List<GuideCategory>>(listOf())
        private set
    var selectedCategory by mutableStateOf<GuideCategory?>(null)
        private set
    var guides by mutableStateOf<List<Guide>>(listOf())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<Throwable?>(null)
        private set

    private var disposables = CompositeDisposable()

    init {
        repository.guideCategories
                .subscribe {
                    viewModelScope.launch {
                        loading = it is DataState.Loading

                        if (it is DataState.Success) {
                            didFetchGuideCategories(it.data)
                        }

                        error = (it as? DataState.Error)?.throwable
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    fun onSelectCategory(category: GuideCategory) {
        selectedCategory = category
        guides = category.guides
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
