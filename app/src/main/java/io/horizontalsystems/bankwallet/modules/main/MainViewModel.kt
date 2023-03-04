package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
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
    private val torManager: ITorManager,
    wc2SessionManager: WC2SessionManager,
    private val wc1Manager: WC1Manager,
    private val wcDeepLink: String?
) : ViewModel() {

    val showRootedDeviceWarningLiveEvent = SingleLiveEvent<Unit>()
    val showRateAppLiveEvent = SingleLiveEvent<Unit>()
    val showWhatsNewLiveEvent = SingleLiveEvent<Unit>()
    val openPlayMarketLiveEvent = SingleLiveEvent<Unit>()
    val hideContentLiveData = MutableLiveData<Boolean>()
    val settingsBadgeLiveData = MutableLiveData<MainModule.BadgeType?>(null)
    val transactionTabEnabledLiveData = MutableLiveData<Boolean>()
    val marketsTabEnabledLiveData = MutableLiveData<Boolean>()
    val openWalletSwitcherLiveEvent = SingleLiveEvent<Pair<List<Account>, Account?>>()
    val torIsActiveLiveData = MutableLiveData(false)
    val playTorActiveAnimationLiveData = MutableLiveData(false)
    val walletConnectSupportState = MutableLiveData<WC1Manager.SupportState?>()

    private val disposables = CompositeDisposable()
    private var contentHidden = pinComponent.isLocked
    private var wc2PendingRequestsCount = 0

    val initialTab = getTabToOpen()

    init {
        viewModelScope.launch {
            service.marketsTabEnabledFlow.collect {
                marketsTabEnabledLiveData.postValue(it)
            }
        }

        if (!service.ignoreRootCheck && service.isDeviceRooted) {
            showRootedDeviceWarningLiveEvent.call()
        }

        updateSettingsBadge()
        updateTransactionsTabEnabled()

        viewModelScope.launch {
            termsManager.termsAcceptedSignalFlow.collect {
                updateSettingsBadge()
            }
        }

        viewModelScope.launch {
            torManager.torStatusFlow.collect { connectionStatus ->
                if (torIsActiveLiveData.value == false && connectionStatus == TorStatus.Connected) {
                    playTorActiveAnimationLiveData.postValue(true)
                }
                torIsActiveLiveData.postValue(connectionStatus == TorStatus.Connected)
            }
        }

        viewModelScope.launch {
            wc2SessionManager.pendingRequestCountFlow.collect {
                wc2PendingRequestsCount = it
                updateSettingsBadge()
            }
        }

        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            updateSettingsBadge()
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            updateSettingsBadge()
        })

        disposables.add(accountManager.accountsFlowable.subscribe {
            updateTransactionsTabEnabled()
            updateSettingsBadge()
        })

        rateAppManager.showRateAppObservable
                .subscribe {
                    showRateApp(it)
                }
                .let {
                    disposables.add(it)
                }

        wcDeepLink?.let {
            val wcSupportState = wc1Manager.getWalletConnectSupportState()
            walletConnectSupportState.postValue(wcSupportState)
        }

        showWhatsNew()
    }

    private fun getTabToOpen() = when {
        wcDeepLink != null -> {
            MainModule.MainTab.Settings
        }
        service.relaunchBySettingChange -> {
            service.relaunchBySettingChange = false
            MainModule.MainTab.Settings
        }
        !service.marketsTabEnabledFlow.value -> {
            MainModule.MainTab.Balance
        }
        else -> when (service.launchPage) {
            LaunchPage.Market,
            LaunchPage.Watchlist -> MainModule.MainTab.Market
            LaunchPage.Balance -> MainModule.MainTab.Balance
            LaunchPage.Auto -> service.currentMainTab ?: MainModule.MainTab.Balance
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onLongPressBalanceTab() {
        openWalletSwitcherLiveEvent.postValue(Pair(accountManager.accounts, accountManager.activeAccount))
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

    fun animationPlayed() {
        playTorActiveAnimationLiveData.postValue(false)
    }

    fun wcSupportStateHandled() {
        walletConnectSupportState.postValue(null)
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

    private fun updateSettingsBadge() {
        val showDotBadge = !(backupManager.allBackedUp && termsManager.allTermsAccepted && pinComponent.isPinSet) || accountManager.hasNonStandardAccount

        if (wc2PendingRequestsCount > 0) {
            settingsBadgeLiveData.postValue(MainModule.BadgeType.BadgeNumber(wc2PendingRequestsCount))
        } else if (showDotBadge) {
            settingsBadgeLiveData.postValue(MainModule.BadgeType.BadgeDot)
        } else {
            settingsBadgeLiveData.postValue(null)
        }
    }

}
