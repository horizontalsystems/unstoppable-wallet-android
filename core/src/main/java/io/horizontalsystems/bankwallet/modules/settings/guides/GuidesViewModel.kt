package io.horizontalsystems.bankwallet.modules.settings.guides

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class GuidesViewModel(private val repository: GuidesRepository) : ViewModelUiState<GuidesUiState>() {
    private var viewState: ViewState = ViewState.Loading
    private var categories = listOf<GuideCategory>()
    private var selectedCategory: GuideCategory? = null
    private var expandedSections = setOf<String>()

    override fun createState() = GuidesUiState(
        viewState = viewState,
        categories = categories,
        selectedCategory = selectedCategory,
        expandedSections = expandedSections
    )

    init {
        viewModelScope.launch {
            repository.guideCategories.asFlow().collect { dataState ->
                viewModelScope.launch {
                    dataState.viewState?.let {
                        viewState = it
                        emitState()
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
        emitState()
    }

    fun toggleSection(sectionTitle: String, expanded: Boolean) {
        expandedSections = if (expanded) {
            expandedSections.minus(sectionTitle)
        } else {
            expandedSections.plus(sectionTitle)
        }

        emitState()
    }

    override fun onCleared() {
        repository.clear()
    }

    private fun didFetchGuideCategories(guideCategories: List<GuideCategory>) {
        categories = guideCategories
        selectedCategory = guideCategories.first()

        emitState()
    }
}

data class GuidesUiState(
    val viewState: ViewState,
    val categories: List<GuideCategory>,
    val selectedCategory: GuideCategory?,
    val expandedSections: Set<String>
)
