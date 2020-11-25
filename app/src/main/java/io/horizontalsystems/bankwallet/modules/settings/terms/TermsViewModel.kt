package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.Term

class TermsViewModel(private val termsManager: ITermsManager) : ViewModel() {

    val termsLiveData = MutableLiveData<List<Term>>()

    private var terms: List<Term> = termsManager.terms

    init {
        termsLiveData.postValue(terms)
    }

    fun onTapTerm(index: Int) {
        terms[index].checked = !terms[index].checked
        termsManager.update(terms[index])
        termsLiveData.postValue(terms)
    }

}
