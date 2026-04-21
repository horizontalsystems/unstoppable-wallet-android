package cash.p.terminal.modules.settings.advancedsecurity.terms

import cash.p.terminal.ui_compose.entities.TermItem
import io.horizontalsystems.core.ViewModelUiState

class HiddenWalletTermsViewModel(
    termTitles: Array<String>
) : ViewModelUiState<HiddenWalletTermsUiState>() {

    private val checklist = TermsChecklist(termTitles)

    override fun createState(): HiddenWalletTermsUiState {
        val state = checklist.state()
        return HiddenWalletTermsUiState(
            terms = state.terms,
            agreeEnabled = state.agreeEnabled
        )
    }

    fun toggleCheckbox(id: Int) {
        checklist.toggle(id)
        emitState()
    }
}

data class HiddenWalletTermsUiState(
    val terms: List<TermItem>,
    val agreeEnabled: Boolean
)
