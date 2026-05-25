package io.horizontalsystems.bankwallet.modules.multiswap.swapterms

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.SwapTermType
import io.horizontalsystems.bankwallet.core.managers.SwapTermsManager
import javax.inject.Inject

@HiltViewModel
class SwapTermsViewModel @Inject constructor(private val swapTermsManager: SwapTermsManager) :
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

