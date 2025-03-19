package io.horizontalsystems.bankwallet.modules.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectMainFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WCListFragment
import io.horizontalsystems.core.IPinComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class MainViewModel(
    private val pinComponent: IPinComponent,
    rateAppManager: IRateAppManager,
    private val backupManager: IBackupManager,
    private val termsManager: ITermsManager,
    private val accountManager: IAccountManager,
    private val releaseNotesManager: ReleaseNotesManager,
    private val localStorage: ILocalStorage,
    wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    private val networkManager: INetworkManager
) : ViewModelUiState<MainModule.UiState>() {

    private var wcPendingRequestsCount = 0
    private var marketsTabEnabled = localStorage.marketsTabEnabledFlow.value
    private var transactionsEnabled = isTransactionsTabEnabled()
    private var settingsBadge: MainModule.BadgeType? = null
    private val launchPage: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    private var currentMainTab: MainNavigation
        get() = localStorage.mainTab ?: MainNavigation.Balance
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

    private var selectedTabIndex = getTabIndexToOpen()
    private var deeplinkPage: DeeplinkPage? = null
    private var mainNavItems = navigationItems()
    private var showRateAppDialog = false
    private var showWhatsNew = false
    private var activeWallet = accountManager.activeAccount
    private var wcSupportState: WCManager.SupportState? = null
    private var torEnabled = localStorage.torEnabled

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    init {
        localStorage.marketsTabEnabledFlow.collectWith(viewModelScope) {
            marketsTabEnabled = it
            syncNavigation()
        }

        termsManager.termsAcceptedSignalFlow.collectWith(viewModelScope) {
            updateSettingsBadge()
        }

        wcSessionManager.pendingRequestCountFlow.collectWith(viewModelScope) {
            wcPendingRequestsCount = it
            updateSettingsBadge()
        }

        rateAppManager.showRateAppFlow.collectWith(viewModelScope) {
            showRateAppDialog = it
            emitState()
        }

        viewModelScope.launch {
            backupManager.allBackedUpFlowable.asFlow().collect {
                updateSettingsBadge()
            }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                updateSettingsBadge()
            }
        }
        viewModelScope.launch {
            accountManager.accountsFlowable.asFlow().collect {
                updateTransactionsTabEnabled()
                updateSettingsBadge()
            }
        }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow.collect {
                if (it is ActiveAccountState.ActiveAccount) {
                    updateTransactionsTabEnabled()
                }
            }
        }

        accountManager.activeAccountStateFlow.collectWith(viewModelScope) {
            (it as? ActiveAccountState.ActiveAccount)?.let { state ->
                activeWallet = state.account
                emitState()
            }
        }

        updateSettingsBadge()
        updateTransactionsTabEnabled()
    }

    override fun createState() = MainModule.UiState(
        selectedTabIndex = selectedTabIndex,
        deeplinkPage = deeplinkPage,
        mainNavItems = mainNavItems,
        showRateAppDialog = showRateAppDialog,
        showWhatsNew = showWhatsNew,
        activeWallet = activeWallet,
        wcSupportState = wcSupportState,
        torEnabled = torEnabled
    )

    private fun isTransactionsTabEnabled(): Boolean =
        !accountManager.isAccountsEmpty && accountManager.activeAccount?.type !is AccountType.Cex


    fun whatsNewShown() {
        showWhatsNew = false
        emitState()
    }

    fun closeRateDialog() {
        showRateAppDialog = false
        emitState()
    }

    fun onSelect(account: Account) {
        accountManager.setActiveAccountId(account.id)
        activeWallet = account
        emitState()
    }

    fun onResume() {
        viewModelScope.launch {
            if (!pinComponent.isLocked && releaseNotesManager.shouldShowChangeLog()) {
                showWhatsNew()
            }
        }
    }

    fun onSelect(mainNavItem: MainNavigation) {
        if (mainNavItem != MainNavigation.Settings) {
            currentMainTab = mainNavItem
        }
        selectedTabIndex = items.indexOf(mainNavItem)
        syncNavigation()
    }

    private fun updateTransactionsTabEnabled() {
        transactionsEnabled = isTransactionsTabEnabled()
        syncNavigation()
    }

    fun wcSupportStateHandled() {
        wcSupportState = null
        emitState()
    }

    private fun navigationItems(): List<MainModule.NavigationViewItem> {
        return items.mapIndexed { index, mainNavItem ->
            getNavItem(mainNavItem, index == selectedTabIndex)
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

    private fun getTabIndexToOpen(): Int {
        val tab = when {
            relaunchBySettingChange -> {
                relaunchBySettingChange = false
                MainNavigation.Settings
            }

            !marketsTabEnabled -> {
                MainNavigation.Balance
            }

            else -> getLaunchTab()
        }

        return items.indexOf(tab)
    }

    private fun getLaunchTab(): MainNavigation = when (launchPage) {
        LaunchPage.Market,
        LaunchPage.Watchlist -> MainNavigation.Market

        LaunchPage.Balance -> MainNavigation.Balance
        LaunchPage.Auto -> currentMainTab
    }

    private fun getNavigationDataForDeeplink(deepLink: Uri): Pair<MainNavigation, DeeplinkPage?> {
        var tab = currentMainTab
        var deeplinkPage: DeeplinkPage? = null
        val deeplinkString = deepLink.toString()
        val deeplinkScheme: String = Translator.getString(R.string.DeeplinkScheme)
        when {
            deeplinkString.startsWith("$deeplinkScheme:") -> {
                val uid = deepLink.getQueryParameter("uid")
                when {
                    deeplinkString.contains("coin-page") -> {
                        uid?.let {
                            deeplinkPage = DeeplinkPage(R.id.coinFragment, CoinFragment.Input(it))

                            stat(page = StatPage.Widget, event = StatEvent.OpenCoin(it))
                        }
                    }

                    deeplinkString.contains("top-platforms") -> {
                        val title = deepLink.getQueryParameter("title")
                        if (title != null && uid != null) {
                            val platform = Platform(uid, title)
                            deeplinkPage = DeeplinkPage(R.id.marketPlatformFragment, platform)

                            stat(page = StatPage.Widget, event = StatEvent.Open(StatPage.TopPlatform))
                        }
                    }
                }

                tab = MainNavigation.Market
            }

            deeplinkString.startsWith("wc:") -> {
                wcSupportState = wcManager.getWalletConnectSupportState()
                if (wcSupportState == WCManager.SupportState.Supported) {
                    deeplinkPage = DeeplinkPage(R.id.wcListFragment, WCListFragment.Input(deeplinkString))
                    tab = MainNavigation.Settings
                }
            }

            deeplinkString.startsWith("unstoppable.money:") ||
            deeplinkString.startsWith("tc:") -> {
                val v = deepLink.getQueryParameter("v")?.toIntOrNull()
                if (v != null) {
                    deeplinkPage = DeeplinkPage(R.id.tcListFragment, TonConnectMainFragment.Input(deeplinkString))
                    tab = MainNavigation.Settings
                }
            }

            deeplinkString.startsWith("https://unstoppable.money/referral") -> {
                val userId: String? = deepLink.getQueryParameter("userId")
                val referralCode: String? = deepLink.getQueryParameter("referralCode")
                if (userId != null && referralCode != null) {
                    registerApp(userId, referralCode)
                }
            }

            else -> {}
        }
        return Pair(tab, deeplinkPage)
    }

    private fun registerApp(userId: String, referralCode: String) {
        viewModelScope.launch {
            try {
                val response = networkManager.registerApp(userId, referralCode)
                if (response.success) {
                    //do nothing
                } else {
                    Log.e("MainViewModel", "registerApp api fail message: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "registerApp error: ", e)
            }
        }
    }

    private fun syncNavigation() {
        mainNavItems = navigationItems()
        if (selectedTabIndex >= mainNavItems.size) {
            selectedTabIndex = mainNavItems.size - 1
        }
        emitState()
    }

    private suspend fun showWhatsNew() {
        delay(2000)
        showWhatsNew = true
        emitState()
    }

    private fun updateSettingsBadge() {
        val showDotBadge =
            !(backupManager.allBackedUp && termsManager.allTermsAccepted && pinComponent.isPinSet) || accountManager.hasNonStandardAccount

        settingsBadge = if (wcPendingRequestsCount > 0) {
            MainModule.BadgeType.BadgeNumber(wcPendingRequestsCount)
        } else if (showDotBadge) {
            MainModule.BadgeType.BadgeDot
        } else {
            null
        }
        syncNavigation()
    }

    fun deeplinkPageHandled() {
        deeplinkPage = null
        emitState()
    }

    fun handleDeepLink(uri: Uri) {
        val (tab, deeplinkPageData) = getNavigationDataForDeeplink(uri)
        deeplinkPage = deeplinkPageData
        currentMainTab = tab
        selectedTabIndex = items.indexOf(tab)
        syncNavigation()
    }

}
