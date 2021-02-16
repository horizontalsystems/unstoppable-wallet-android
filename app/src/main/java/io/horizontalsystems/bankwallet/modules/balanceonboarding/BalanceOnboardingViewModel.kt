package io.horizontalsystems.bankwallet.modules.balanceonboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Account
import io.reactivex.disposables.CompositeDisposable

class BalanceOnboardingViewModel(accountManager: IAccountManager): ViewModel() {

    val hasAccountsLiveData = MutableLiveData<Boolean>()

    val disposables = CompositeDisposable()

    init {
        disposables.add(accountManager.accountsFlowable.subscribe {
            sync(it)
        })

        sync(accountManager.accounts)
    }

    fun sync(accounts: List<Account>) {
        hasAccountsLiveData.postValue(accounts.isNotEmpty())
    }

    override fun onCleared() {
        disposables.clear()
    }
}
