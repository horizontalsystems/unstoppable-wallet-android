package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.bankwallet.entities.ViewState
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class GuidesViewModel(private val repository: GuidesRepository) : ViewModel() {

    var categories by mutableStateOf<List<GuideCategory>>(listOf())
        private set
    var selectedCategory by mutableStateOf<GuideCategory?>(null)
        private set
    var guides by mutableStateOf<List<Guide>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    private var disposables = CompositeDisposable()

    init {
        repository.guideCategories
                .subscribe { dataState ->
                    viewModelScope.launch {
                        dataState.viewState?.let {
                            viewState = it
                        }

                        if (dataState is DataState.Success) {
                            didFetchGuideCategories(dataState.data)
                        }
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

    private fun didFetchGuideCategories(guideCategories: List<GuideCategory>) {
        categories = guideCategories
        onSelectCategory(guideCategories.first())
    }

}
