package cash.p.terminal.modules.settings.advancedsecurity.securereset

import cash.p.terminal.core.IBackupManager
import cash.p.terminal.modules.settings.advancedsecurity.terms.TermsChecklist
import cash.p.terminal.ui_compose.entities.TermItem
import io.horizontalsystems.core.ViewModelUiState

class SecureResetTermsViewModel(
    termTitles: Array<String>,
    private val backupManager: IBackupManager
) : ViewModelUiState<SecureResetTermsUiState>() {

    private val checklist = TermsChecklist(termTitles)

    override fun createState(): SecureResetTermsUiState {
        val state = checklist.state()
        return SecureResetTermsUiState(
            terms = state.terms,
            agreeEnabled = state.agreeEnabled,
            allBackedUp = backupManager.allBackedUp
        )
    }

    fun toggleCheckbox(id: Int) {
        checklist.toggle(id)
        emitState()
    }
}

data class SecureResetTermsUiState(
    val terms: List<TermItem>,
    val agreeEnabled: Boolean,
    val allBackedUp: Boolean
)
