package io.horizontalsystems.bankwallet.modules.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.settings.security.tor.TorStatus
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(
    private val pinComponent: IPinComponent,
    rateAppManager: IRateAppManager,
    private val backupManager: IBackupManager,
    private val termsManager: ITermsManager,
    private val accountManager: IAccountManager,
    private val releaseNotesManager: ReleaseNotesManager,
    private val localStorage: ILocalStorage,
    torManager: ITorManager,
    wc2SessionManager: WC2SessionManager,
    private val wc1Manager: WC1Manager,
    private val wcDeepLink: String?
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var wc2PendingRequestsCount = 0
    private var marketsTabEnabled = localStorage.marketsTabEnabledFlow.value
    private var transactionsEnabled = !accountManager.isAccountsEmpty
    private var settingsBadge: MainModule.BadgeType? = null
    private val launchPage: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    private var currentMainTab: MainNavigation?
        get() = localStorage.mainTab
        set(value) {
            localStorage.mainTab = value
        }

    private var relaunchBySettingChange: Boolean
        get() = localStorage.relaunchBySettingChange
        set(value) {
            localStorage.relaunchBySettingChange = value
        }

    private val items: List<MainNavigation>
        get() = if (marketsTabEnabled) {
            listOf(
                MainNavigation.Market,
                MainNavigation.Balance,
                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        } else {
            listOf(
                MainNavigation.Balance,
                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        }

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    var selectedPageIndex by mutableStateOf(getPageIndexToOpen())
        private set

    var mainNavItems by mutableStateOf(navigationItems())
        private set

    var showRateAppDialog by mutableStateOf(false)
        private set

    var contentHidden by mutableStateOf(pinComponent.isLocked)
        private set

    var showWhatsNew by mutableStateOf(false)
        private set

    var activeWallet by mutableStateOf(accountManager.activeAccount)
        private set

    var torIsActive by mutableStateOf(false)
        private set

    var wcSupportState by mutableStateOf<WC1Manager.SupportState?>(null)
        private set


    init {
        localStorage.marketsTabEnabledFlow.collectWith(viewModelScope) {
            marketsTabEnabled = it
            syncNavigation()
        }

        termsManager.termsAcceptedSignalFlow.collectWith(viewModelScope) {
            updateSettingsBadge()
        }

        torManager.torStatusFlow.collectWith(viewModelScope) { connectionStatus ->
            torIsActive = connectionStatus == TorStatus.Connected
        }

        wc2SessionManager.pendingRequestCountFlow.collectWith(viewModelScope) {
            wc2PendingRequestsCount = it
            updateSettingsBadge()
        }

        rateAppManager.showRateAppFlow.collectWith(viewModelScope) {
            showRateAppDialog = it
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


        wcDeepLink?.let {
            wcSupportState = wc1Manager.getWalletConnectSupportState()
        }

        accountManager.activeAccountStateFlow.collectWith(viewModelScope) {
            (it as? ActiveAccountState.ActiveAccount)?.let { state ->
                activeWallet = state.account
            }
        }

        updateSettingsBadge()
        updateTransactionsTabEnabled()
        showWhatsNew()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun whatsNewShown() {
        showWhatsNew = false
    }

    fun closeRateDialog() {
        showRateAppDialog = false
    }

    fun onSelect(account: Account) {
        accountManager.setActiveAccountId(account.id)
        activeWallet = account
    }

    fun onResume() {
        contentHidden = pinComponent.isLocked
    }

    fun onSelect(mainNavItem: MainNavigation) {
        if (mainNavItem != MainNavigation.Settings) {
            currentMainTab = mainNavItem
        }
        selectedPageIndex = items.indexOf(mainNavItem)
        syncNavigation()
    }

    fun updateTransactionsTabEnabled() {
        transactionsEnabled = !accountManager.isAccountsEmpty
        syncNavigation()
    }

    fun wcSupportStateHandled() {
        wcSupportState = null
    }

    private fun navigationItems(): List<MainModule.NavigationViewItem> {
        return items.mapIndexed { index, mainNavItem ->
            getNavItem(mainNavItem, index == selectedPageIndex)
        }
    }

    private fun getNavItem(item: MainNavigation, selected: Boolean) = when (item) {
        MainNavigation.Market -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
            )
        }
        MainNavigation.Transactions -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = transactionsEnabled,
            )
        }
        MainNavigation.Settings -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
                badge = settingsBadge
            )
        }
        MainNavigation.Balance -> {
            MainModule.NavigationViewItem(
                mainNavItem = item,
                selected = selected,
                enabled = true,
            )
        }
    }

    private fun getPageIndexToOpen(): Int {
        val page = when {
            wcDeepLink != null -> {
                MainNavigation.Settings
            }
            relaunchBySettingChange -> {
                relaunchBySettingChange = false
                MainNavigation.Settings
            }
            !marketsTabEnabled -> {
                MainNavigation.Balance
            }
            else -> when (launchPage) {
                LaunchPage.Market,
                LaunchPage.Watchlist -> MainNavigation.Market
                LaunchPage.Balance -> MainNavigation.Balance
                LaunchPage.Auto -> currentMainTab ?: MainNavigation.Balance
            }
        }
        return items.indexOf(page)
    }

    private fun syncNavigation() {
        mainNavItems = navigationItems()
    }

    private fun showWhatsNew() {
        viewModelScope.launch {
            if (releaseNotesManager.shouldShowChangeLog()) {
                delay(2000)
                showWhatsNew = true
            }
        }
    }

    private fun updateSettingsBadge() {
        val showDotBadge =
            !(backupManager.allBackedUp && termsManager.allTermsAccepted && pinComponent.isPinSet) || accountManager.hasNonStandardAccount

        settingsBadge = if (wc2PendingRequestsCount > 0) {
            MainModule.BadgeType.BadgeNumber(wc2PendingRequestsCount)
        } else if (showDotBadge) {
            MainModule.BadgeType.BadgeDot
        } else {
            null
        }
        syncNavigation()
    }

}
