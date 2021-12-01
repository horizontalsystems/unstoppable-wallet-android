package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
    private val pinComponent: IPinComponent,
    rateAppManager: IRateAppManager,
    private val backupManager: IBackupManager,
    private val termsManager: ITermsManager,
    private val accountManager: IAccountManager,
    private val releaseNotesManager: ReleaseNotesManager,
    private val service: MainService,
) : ViewModel() {

    val showRootedDeviceWarningLiveEvent = SingleLiveEvent<Unit>()
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val showWhatsNewLiveEvent = SingleLiveEvent<Unit>()
    val openPlayMarketLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()
    val setBadgeVisibleLiveData = MutableLiveData<Boolean>()
    val transactionTabEnabledLiveData = MutableLiveData<Boolean>()
    val openWalletSwitcherLiveEvent = SingleLiveEvent<Pair<List<ViewItemWrapper<Account>>,ViewItemWrapper<Account>?>>()

    private val disposables = CompositeDisposable()
    private var contentHidden = pinComponent.isLocked

    val initialTab = getTabToOpen()

    init {

        if (!service.ignoreRootCheck && service.isDeviceRooted) {
            showRootedDeviceWarningLiveEvent.call()
        }

        updateBadgeVisibility()
        updateTransactionsTabEnabled()

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
            updateTransactionsTabEnabled()
        })

        rateAppManager.showRateAppObservable
                .subscribe {
                    showRateApp(it)
                }
                .let {
                    disposables.add(it)
                }

        showWhatsNew()
    }

    private fun getTabToOpen(): MainModule.MainTab {
        if(service.relaunchBySettingChange){
            service.relaunchBySettingChange = false
            return MainModule.MainTab.Settings
        }
        return when(service.launchPage){
            LaunchPage.Market,
            LaunchPage.Watchlist -> MainModule.MainTab.Market
            LaunchPage.Balance -> MainModule.MainTab.Balance
            LaunchPage.Auto -> service.currentMainTab ?: MainModule.MainTab.Market
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onLongPressBalanceTab() {
        val accounts = accountManager.accounts.map { account ->
            ViewItemWrapper(account.name, account, subtitle = account.type.description)
        }
        val activeAccount = accounts.find { account -> account.item == accountManager.activeAccount }

        openWalletSwitcherLiveEvent.postValue(Pair(accounts, activeAccount))
    }

    fun onSelect(account: Account) {
        accountManager.setActiveAccountId(account.id)
    }

    fun onResume() {
        if (contentHidden != pinComponent.isLocked) {
            hideContentLiveData.postValue(pinComponent.isLocked)
        }
        contentHidden = pinComponent.isLocked
    }

    fun onTabSelect(tabIndex: Int) {
        val mainTab = MainModule.MainTab.values()[tabIndex]
        if(mainTab != MainModule.MainTab.Settings){
            service.currentMainTab = mainTab
        }
    }

    fun updateTransactionsTabEnabled() {
        transactionTabEnabledLiveData.postValue(!accountManager.isAccountsEmpty)
    }

    private fun showWhatsNew() {
        viewModelScope.launch(Dispatchers.IO){
            if (releaseNotesManager.shouldShowChangeLog()){
                delay(2000)
                showWhatsNewLiveEvent.postValue(Unit)
            }
        }
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
