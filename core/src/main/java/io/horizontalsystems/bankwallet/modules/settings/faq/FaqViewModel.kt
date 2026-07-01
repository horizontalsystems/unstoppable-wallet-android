package io.horizontalsystems.bankwallet.modules.settings.faq

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.Faq
import io.horizontalsystems.bankwallet.entities.FaqSection
import io.horizontalsystems.bankwallet.entities.ViewState
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class FaqViewModel(private val repository: FaqRepository) : ViewModel() {

    var sections by mutableStateOf<List<FaqSection>>(listOf())
        private set
    var selectedSection by mutableStateOf<FaqSection?>(null)
        private set
    var faqItems by mutableStateOf<List<Faq>>(listOf())
        private set
    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    init {
        viewModelScope.launch {
            repository.faqList.asFlow().collect { dataState ->
                viewModelScope.launch {
                    dataState.viewState?.let {
                        viewState = it
                    }

                    if (dataState is DataState.Success) {
                        didFetchFaqSections(dataState.data)
                    }
                }
            }
        }

        repository.start()
    }

    fun onSelectSection(section: FaqSection) {
        selectedSection = section
        faqItems = section.faqItems
    }

    override fun onCleared() {
        repository.clear()
    }

    private fun didFetchFaqSections(faqSections: List<FaqSection>) {
        sections = faqSections
        onSelectSection(faqSections.first())
    }
}
