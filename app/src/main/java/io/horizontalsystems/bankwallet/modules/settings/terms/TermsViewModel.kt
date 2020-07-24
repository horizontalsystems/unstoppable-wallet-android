package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.core.SingleLiveEvent

class TermsViewModel(private val termsManager: ITermsManager, private val appConfigProvider: IAppConfigProvider) : ViewModel() {

    val termsLiveData = MutableLiveData<List<Term>>()
    val openLink = SingleLiveEvent<String>()

    private var terms: List<Term> = termsManager.terms

    init {
        termsLiveData.postValue(terms)
    }

    fun onTapTerm(index: Int) {
        terms[index].checked = !terms[index].checked
        termsManager.update(terms[index])
        termsLiveData.postValue(terms)
    }

    fun onGithubButtonClick() {
        openLink.postValue(appConfigProvider.appGithubLink)
    }

    fun onSiteButtonClick() {
        openLink.postValue(appConfigProvider.appWebPageLink)
    }
}
