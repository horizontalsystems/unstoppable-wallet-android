package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule.TermViewItem

class TermsViewModel(private val termsManager: ITermsManager) : ViewModel() {

    private val terms by termsManager::terms

    private var checkedTerms by mutableStateOf(setOf<TermsModule.TermType>())

    val readOnlyState = termsManager.allTermsAccepted

    var closeWithTermsAgreed by mutableStateOf(false)
        private set

    val termsViewItems by derivedStateOf {
        terms.map { termType ->
            TermViewItem(termType, checkedTerms.contains(termType))
        }
    }

    val buttonEnabled by derivedStateOf {
        checkedTerms.containsAll(terms)
    }

    var isAcceptButtonVisible by mutableStateOf(!readOnlyState)

    init {
        val initialCheckedTerms = if (readOnlyState) {
            terms.toSet()
        } else {
            terms.filter { term ->
                termsManager.checkedTermIds.contains(term.key)
            }.toSet()
        }

        checkedTerms = initialCheckedTerms
    }

    fun onTapTerm(termType: TermsModule.TermType, checked: Boolean) {
        checkedTerms = if (checked) {
            checkedTerms + termType
        } else {
            checkedTerms - termType
        }
    }

    fun onAgreeClick() {
        if (buttonEnabled) {
            termsManager.acceptTerms()
            closeWithTermsAgreed = true
        }
    }

    fun onTermsAgreedConsumed() {
        closeWithTermsAgreed = false
    }
}
