package cash.p.terminal.modules.settings.guides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.GuideCategory
import cash.p.terminal.entities.ViewState
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class GuidesViewModel(private val repository: GuidesRepository) : ViewModel() {

    var categories by mutableStateOf<List<GuideCategory>>(listOf())
        private set
    var selectedCategory by mutableStateOf<GuideCategory?>(null)
        private set
    var expandedSections by mutableStateOf(setOf<String>())
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
    }

    override fun onCleared() {
        repository.clear()
    }

    private fun didFetchGuideCategories(guideCategories: List<GuideCategory>) {
        categories = guideCategories
        onSelectCategory(guideCategories.first())
    }

    fun toggleSection(sectionTitle: String, expanded: Boolean) {
        expandedSections = if (expanded) {
            expandedSections.minus(sectionTitle)
        } else {
            expandedSections.plus(sectionTitle)
        }
    }

}
