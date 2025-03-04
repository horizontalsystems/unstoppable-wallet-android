package cash.p.terminal.modules.main

import android.net.Uri
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.R
import cash.p.terminal.core.IBackupManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IRateAppManager
import cash.p.terminal.core.ITermsManager
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.modules.main.MainModule.MainNavigation
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.modules.nft.collection.NftCollectionFragment
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import cash.p.terminal.modules.walletconnect.list.WCListFragment
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.ActiveAccountState
import cash.p.terminal.wallet.IAccountManager
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
                MainNavigation.Balance,
                MainNavigation.Transactions,
                MainNavigation.Market,
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
    private var contentHidden = false
    private var showWhatsNew = false
    private var activeWallet = accountManager.activeAccount
    private var wcSupportState: WCManager.SupportState? = null
    private var torEnabled = localStorage.torEnabled

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    init {
        pinComponent.isLocked.collectWith(viewModelScope) {
            contentHidden = it
            emitState()
        }

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
        contentHidden = contentHidden,
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
        emitState()
        viewModelScope.launch {
            if (!pinComponent.isLocked.value && releaseNotesManager.shouldShowChangeLog()) {
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
        val deeplinkScheme: String =
            cash.p.terminal.strings.helpers.Translator.getString(R.string.DeeplinkScheme)
        when {
            deeplinkString.startsWith("$deeplinkScheme:") -> {
                val uid = deepLink.getQueryParameter("uid")
                when {
                    deeplinkString.contains("coin-page") -> {
                        uid?.let {
                            deeplinkPage = DeeplinkPage(R.id.coinFragment, CoinFragmentInput(it))

                            stat(page = StatPage.Widget, event = StatEvent.OpenCoin(it))
                        }
                    }

                    deeplinkString.contains("nft-collection") -> {
                        val blockchainTypeUid = deepLink.getQueryParameter("blockchainTypeUid")
                        if (uid != null && blockchainTypeUid != null) {
                            deeplinkPage = DeeplinkPage(
                                R.id.nftCollectionFragment,
                                NftCollectionFragment.Input(uid, blockchainTypeUid)
                            )

                            stat(
                                page = StatPage.Widget,
                                event = StatEvent.Open(StatPage.TopNftCollections)
                            )
                        }
                    }

                    deeplinkString.contains("top-platforms") -> {
                        val title = deepLink.getQueryParameter("title")
                        if (title != null && uid != null) {
                            val platform = Platform(uid, title)
                            deeplinkPage = DeeplinkPage(R.id.marketPlatformFragment, platform)

                            stat(
                                page = StatPage.Widget,
                                event = StatEvent.Open(StatPage.TopPlatform)
                            )
                        }
                    }
                }

                tab = MainNavigation.Market
            }

            deeplinkString.startsWith("wc:") -> {
                wcSupportState = wcManager.getWalletConnectSupportState()
                if (wcSupportState == WCManager.SupportState.Supported) {
                    deeplinkPage =
                        DeeplinkPage(R.id.wcListFragment, WCListFragment.Input(deeplinkString))
                    tab = MainNavigation.Settings
                }
            }

            else -> {}
        }
        return Pair(tab, deeplinkPage)
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
