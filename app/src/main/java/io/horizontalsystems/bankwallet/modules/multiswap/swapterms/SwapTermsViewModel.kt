package io.horizontalsystems.bankwallet.modules.multiswap.swapterms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.SwapTermType
import io.horizontalsystems.bankwallet.core.managers.SwapTermsManager

class SwapTermsViewModel(private val swapTermsManager: SwapTermsManager) :
    ViewModelUiState<UiState>() {

    private val terms = swapTermsManager.terms
    private val checkboxStates = mutableListOf<Boolean>().apply {
        addAll(List(terms.size) { false })
    }

    private var buttonEnabled = false

    override fun createState(): UiState {
        return UiState(
            terms,
            checkboxStates.toList(),
            buttonEnabled
        )
    }

    fun toggleTerm(index: Int) {
        checkboxStates[index] = !checkboxStates[index]
        buttonEnabled = checkboxStates.all { it }
        emitState()
    }

    fun onConfirm() {
        swapTermsManager.acceptTerms()
    }
}

data class UiState(
    val terms: List<SwapTermType>,
    val checkboxStates: List<Boolean>,
    val buttonEnabled: Boolean
)

object SwapTermsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapTermsViewModel(App.swapTermsManager) as T
        }
    }
}
