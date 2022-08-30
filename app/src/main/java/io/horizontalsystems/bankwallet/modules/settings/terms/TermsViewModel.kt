package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule.TermViewItem

class TermsViewModel(private val termsManager: ITermsManager) : ViewModel() {

    private var terms: List<Term> = termsManager.terms

    var termsViewItems by mutableStateOf(viewItems(terms))
        private set

    var buttonEnabled by mutableStateOf(termsManager.termsAccepted)
        private set

    fun onTapTerm(termType: TermsModule.TermType, checked: Boolean) {
        terms.firstOrNull { it.id == termType.key }?.let { term ->
            term.checked = checked
            termsManager.update(term)
            termsViewItems = viewItems(terms)
            buttonEnabled = termsManager.termsAccepted
        }
    }

    fun viewItems(terms: List<Term>): List<TermViewItem> {
        return terms.map { term ->
            TermViewItem(TermsModule.TermType.values().first { it.key == term.id }, term.checked)
        }
    }

}
