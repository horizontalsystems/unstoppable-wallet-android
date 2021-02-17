package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class MainViewModel(
        private val pinComponent: IPinComponent,
        rateAppManager: IRateAppManager,
        private val backupManager: IBackupManager,
        private val termsManager: ITermsManager,
        accountManager: IAccountManager
) : ViewModel() {

    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val openPlayMarketLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()
    val setBadgeVisibleLiveData = MutableLiveData<Boolean>()
    val transactionTabEnabledLiveData = MutableLiveData<Boolean>()

    private val disposables = CompositeDisposable()
    private var contentHidden = pinComponent.isLocked

    init {
        updateBadgeVisibility()
        sync(accountManager.accounts)

        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            updateBadgeVisibility()
        })

        disposables.add(accountManager.accountsFlowable.subscribe {
            sync(it)
        })

        rateAppManager.showRateAppObservable
                .subscribe {
                    showRateApp(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onResume() {
        if (contentHidden != pinComponent.isLocked) {
            hideContentLiveData.postValue(pinComponent.isLocked)
        }
        contentHidden = pinComponent.isLocked
    }

    fun sync(accounts: List<Account>) {
        transactionTabEnabledLiveData.postValue(accounts.isNotEmpty())
    }

    private fun showRateApp(showRateUs: RateUsType) {
        when (showRateUs) {
            RateUsType.OpenPlayMarket -> openPlayMarketLiveEvent.postValue(Unit)
            RateUsType.ShowDialog -> showRateAppLiveEvent.postValue(Unit)
        }
    }

    private fun updateBadgeVisibility() {
        val visible = !(backupManager.allBackedUp && termsManager.termsAccepted && pinComponent.isPinSet)
        setBadgeVisibleLiveData.postValue(visible)
    }

}
