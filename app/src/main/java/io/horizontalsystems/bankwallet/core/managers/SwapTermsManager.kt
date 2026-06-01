package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.update

@Singleton
class SwapTermsManager @Inject constructor(private val localStorage: ILocalStorage) {

    private val _termsAcceptedFlow = MutableStateFlow(localStorage.swapTermsAccepted)
    val swapTermsAcceptedStateFlow = _termsAcceptedFlow.asStateFlow()

    val terms = SwapTermType.entries

    fun acceptTerms() {
        localStorage.swapTermsAccepted = true
        _termsAcceptedFlow.update { true }
    }

}

enum class SwapTermType(val title: Int, val description: Int) {
    RiskRestrictions(
        R.string.SwapTerms_RiskRestrictions,
        R.string.SwapTerms_RiskRestrictions_Info
    ),
    UserResponsibility(
        R.string.SwapTerms_UserResponsibility,
        R.string.SwapTerms_UserResponsibility_Info
    ),
}