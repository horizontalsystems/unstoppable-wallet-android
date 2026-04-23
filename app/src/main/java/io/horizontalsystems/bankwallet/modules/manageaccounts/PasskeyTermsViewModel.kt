package io.horizontalsystems.bankwallet.modules.manageaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class PasskeyTermsViewModel(
    private val localStorage: ILocalStorage
) : ViewModelUiState<UiState>() {

    private var checkedTerms = setOf<PasskeyTerm>()
    private var buttonEnabled = false
    private var closeScreen = false

    override fun createState(): UiState {
        return UiState(
            terms = PasskeyTerm.entries.map {
                PasskeyTermItem(it, checkedTerms.contains(it))
            },
            buttonEnabled = buttonEnabled,
            closeScreen = closeScreen
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
        localStorage.passkeyTermsAccepted = true
        closeScreen = true
        emitState()
    }

}

data class UiState(
    val terms: List<PasskeyTermItem>,
    val buttonEnabled: Boolean,
    val closeScreen: Boolean = false,
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

object PasskeyTermsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasskeyTermsViewModel(App.localStorage) as T
        }
    }
}