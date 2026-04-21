package io.horizontalsystems.bankwallet.modules.manageaccounts

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class PasskeyTermsViewModel : ViewModelUiState<UiState>() {

    private var checkedTerms = setOf<PasskeyTerm>()
    private var buttonEnabled = false

    override fun createState(): UiState {
        return UiState(
            terms = PasskeyTerm.entries.map {
                PasskeyTermItem(it, checkedTerms.contains(it))
            },
            buttonEnabled = buttonEnabled
        )
    }

    fun onCheck(term: PasskeyTerm) {
        checkedTerms = if (checkedTerms.contains(term)) {
            checkedTerms - term
        } else {
            checkedTerms + term
        }
        buttonEnabled = checkedTerms.size == PasskeyTerm.entries.size
        emitState()
    }

    fun onContinueClick() {
        TODO("Not yet implemented")
    }

}

data class UiState(
    val terms: List<PasskeyTermItem>,
    val buttonEnabled: Boolean,
)

data class PasskeyTermItem(
    val term: PasskeyTerm,
    val checked: Boolean
)

enum class PasskeyTerm(val title: Int, val description: Int) {
    DeviceDependency(
        R.string.PasskeyTerms_DeviceDependency,
        R.string.PasskeyTerms_DeviceDependency_Description
    ),
    LimitedAccess(
        R.string.PasskeyTerms_LimitedAccess,
        R.string.PasskeyTerms_LimitedAccess_Description
    ),
    Responsibility(
        R.string.PasskeyTerms_Responsibility,
        R.string.PasskeyTerms_Responsibility_Description
    ),
}