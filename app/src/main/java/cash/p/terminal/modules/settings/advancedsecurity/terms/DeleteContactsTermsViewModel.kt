package cash.p.terminal.modules.settings.advancedsecurity.terms

import cash.p.terminal.ui_compose.entities.TermItem
import io.horizontalsystems.core.ViewModelUiState

class DeleteContactsTermsViewModel(
    termTitles: Array<String>
) : ViewModelUiState<DeleteContactsTermsUiState>() {

    private val checklist = TermsChecklist(termTitles)

    override fun createState(): DeleteContactsTermsUiState {
        val state = checklist.state()
        return DeleteContactsTermsUiState(
            terms = state.terms,
            agreeEnabled = state.agreeEnabled
        )
    }

    fun toggleCheckbox(id: Int) {
        checklist.toggle(id)
        emitState()
    }
}

data class DeleteContactsTermsUiState(
    val terms: List<TermItem>,
    val agreeEnabled: Boolean
)
