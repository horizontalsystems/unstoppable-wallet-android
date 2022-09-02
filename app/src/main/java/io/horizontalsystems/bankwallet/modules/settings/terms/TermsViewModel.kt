package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule.TermViewItem

class TermsViewModel(private val termsManager: ITermsManager) : ViewModel() {

    private val terms by termsManager::terms

    private var checkedTerms = mutableListOf<TermsModule.TermType>().also {
        if(termsManager.allTermsAccepted) {
            it.addAll(terms)
        }
    }

    val readOnlyState  = termsManager.allTermsAccepted

    var closeWithTermsAgreed by mutableStateOf(false)
        private set

    var termsViewItems by mutableStateOf(getViewItems())
        private set

    var buttonEnabled by mutableStateOf(buttonEnabled())
        private set

    var buttonVisible by mutableStateOf(!readOnlyState)
        private set


    fun onTapTerm(termType: TermsModule.TermType, checked: Boolean) {
        if (checked) {
            checkedTerms.add(termType)
        } else {
            checkedTerms.remove(termType)
        }

        termsViewItems = getViewItems()
        buttonEnabled = buttonEnabled()
    }

    fun onAgreeClick() {
        termsManager.acceptTerms()
        closeWithTermsAgreed = true
    }

    fun closedWithTermsAgreed() {
        closeWithTermsAgreed = false
    }

    private fun getViewItems() =
        terms.map { termType -> TermViewItem(termType, checkedTerms.any { it == termType }) }

    private fun buttonEnabled() = termsViewItems.all { it.checked }

}
