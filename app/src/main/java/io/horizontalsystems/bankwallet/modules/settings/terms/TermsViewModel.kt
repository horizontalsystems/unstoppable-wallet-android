package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule.TermViewItem

class TermsViewModel(private val termsManager: ITermsManager) : ViewModel() {

    val termsLiveData = MutableLiveData<List<TermViewItem>>()

    private var terms: List<Term> = termsManager.terms

    init {
        syncViewItems()
    }

    private fun syncViewItems() {
        termsLiveData.postValue(viewItems(terms))
    }

    fun onTapTerm(index: Int, checked: Boolean) {
        terms[index].checked = checked
        termsManager.update(terms[index])
        syncViewItems()
    }

    fun viewItems(terms: List<Term>): List<TermViewItem> {
        return terms.map { term ->
            TermViewItem(TermsModule.TermType.values().first { it.key == term.id }, term.checked)
        }
    }

}
