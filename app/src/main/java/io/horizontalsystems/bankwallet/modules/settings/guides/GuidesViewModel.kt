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
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class GuidesViewModel(private val repository: GuidesRepository) : ViewModel() {

    var categories by mutableStateOf<List<GuideCategory>>(listOf())
        private set
    var selectedCategory by mutableStateOf<GuideCategory?>(null)
        private set
    var guides by mutableStateOf<List<Guide>>(listOf())
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    init {
        viewModelScope.launch {
            repository.guideCategories.asFlow().collect { dataState ->
                viewModelScope.launch {
                    dataState.viewState?.let {
                        viewState = it
                    }

                    if (dataState is DataState.Success) {
                        didFetchGuideCategories(dataState.data)
                    }
                }
            }
        }
    }

    fun onSelectCategory(category: GuideCategory) {
        selectedCategory = category
        guides = category.guides
    }

    override fun onCleared() {
        repository.clear()
    }

    private fun didFetchGuideCategories(guideCategories: List<GuideCategory>) {
        categories = guideCategories
        onSelectCategory(guideCategories.first())
    }

}
