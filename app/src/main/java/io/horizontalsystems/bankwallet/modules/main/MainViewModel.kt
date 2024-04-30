package io.horizontalsystems.bankwallet.modules.main

import android.net.Uri
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
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
import io.horizontalsystems.bankwallet.modules.nft.collection.NftCollectionFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WCListFragment
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
    wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    deepLink: Uri?
) : ViewModelUiState<MainModule.UiState>() {

    private val disposables = CompositeDisposable()
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

    private var selectedTabIndex = getTabIndexToOpen(deepLink)
    private var deeplinkPage: DeeplinkPage? = null
    private var mainNavItems = navigationItems()
    private var showRateAppDialog = false
    private var contentHidden = pinComponent.isLocked
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
        showWhatsNew()
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


    override fun onCleared() {
        disposables.clear()
    }

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
        contentHidden = pinComponent.isLocked
        emitState()
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

    private fun getTabIndexToOpen(deepLink: Uri? = null): Int {
        deepLink?.let {
            val (tab, deeplinkPageData) = getNavigationDataForDeeplink(it)
            deeplinkPage = deeplinkPageData
            currentMainTab = tab
            return items.indexOf(tab)
        }

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

                    deeplinkString.contains("nft-collection") -> {
                        val blockchainTypeUid = deepLink.getQueryParameter("blockchainTypeUid")
                        if (uid != null && blockchainTypeUid != null) {
                            deeplinkPage = DeeplinkPage(R.id.nftCollectionFragment, NftCollectionFragment.Input(uid, blockchainTypeUid))

                            stat(page = StatPage.Widget, event = StatEvent.Open(StatPage.TopNftCollections))
                        }
                    }

                    deeplinkString.contains("top-platforms") -> {
                        val title = deepLink.getQueryParameter("title")
                        if (title != null && uid != null) {
                            val platform = Platform(uid, title)
                            deeplinkPage = DeeplinkPage(R.id.marketPlatformFragment, platform)

                            stat(page = StatPage.Widget, event = StatEvent.Open(StatPage.TopPlatforms))
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

    private fun showWhatsNew() {
        viewModelScope.launch {
            if (releaseNotesManager.shouldShowChangeLog()) {
                delay(2000)
                showWhatsNew = true
                emitState()
            }
        }
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

}
