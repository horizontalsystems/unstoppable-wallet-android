package io.horizontalsystems.walletkit.modules.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.IPinComponent
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IBackupManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.INetworkManager
import io.horizontalsystems.walletkit.core.IRateAppManager
import io.horizontalsystems.walletkit.core.ITermsManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.managers.ActionCompletedDelegate
import io.horizontalsystems.walletkit.core.managers.ActiveAccountState
import io.horizontalsystems.walletkit.core.managers.DonationShowManager
import io.horizontalsystems.walletkit.core.managers.ReleaseNotesManager
import io.horizontalsystems.walletkit.core.managers.WalletEventType
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.utils.AddressUriParser
import io.horizontalsystems.walletkit.entities.AddressUri
import io.horizontalsystems.walletkit.entities.LaunchPage
import io.horizontalsystems.walletkit.modules.balance.OpenSendTokenSelect
import io.horizontalsystems.walletkit.modules.coin.CoinPage
import io.horizontalsystems.walletkit.modules.main.MainModule.MainNavigation
import io.horizontalsystems.walletkit.modules.market.platform.MarketPlatformPage
import io.horizontalsystems.walletkit.modules.market.topplatforms.Platform
import io.horizontalsystems.walletkit.modules.pin.core.LockGate
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.WCSessionManager
import io.horizontalsystems.walletkit.modules.walletconnect.list.WCListPage
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

class MainViewModel(
    private val pinComponent: IPinComponent,
    rateAppManager: IRateAppManager,
    private val backupManager: IBackupManager,
    private val termsManager: ITermsManager,
    private val accountManager: IAccountManager,
    private val releaseNotesManager: ReleaseNotesManager,
    private val donationShowManager: DonationShowManager,
    private val localStorage: ILocalStorage,
    wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    private val networkManager: INetworkManager,
    private val actionCompletedDelegate: ActionCompletedDelegate,
    private val lockGate: LockGate,
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
                MainNavigation.Swap,
//                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        } else {
            listOf(
                MainNavigation.Balance,
                MainNavigation.Swap,
//                MainNavigation.Transactions,
                MainNavigation.Settings,
            )
        }
    private val selectedTabItem: MainNavigation
        get() = mainNavItems.firstOrNull { it.selected }?.mainNavItem
            ?: MainNavigation.Balance

    private var selectedTabIndex = getTabIndexToOpen()
    private var deeplinkPage: DeeplinkPage? = null
    private var mainNavItems = navigationItems()
    private var showRateAppDialog = false
    private var showWhatsNew = false
    private var showDonationPage = false
    private var wcSupportState: WCManager.SupportState? = null
    private var torEnabled = localStorage.torEnabled
    private var openSendTokenSelect: OpenSendTokenSelect? = null
    private var snapTabSwitch = false

    // App locked while a public screen was showing: fall back to the Market tab so the user
    // keeps browsing instead of getting the keypad. The stored launch tab is left untouched.
    private val restrictedLockListener: () -> Unit = {
        if (items.contains(MainNavigation.Market)) {
            selectTab(MainNavigation.Market)
        }
    }

    init {
        reportSelectedTab()
        lockGate.addRestrictedLockListener(restrictedLockListener)

        viewModelScope.launch {
            localStorage.marketsTabEnabledFlow.collect { enabled ->
                marketsTabEnabled = enabled
                syncNavigation()
            }
        }

        viewModelScope.launch {
            termsManager.termsAcceptedSharedFlow.collect {
                updateSettingsBadge()
            }
        }

        viewModelScope.launch {
            wcSessionManager.pendingRequestCountFlow.collect {
                wcPendingRequestsCount = it
                updateSettingsBadge()
            }
        }

        viewModelScope.launch {
            rateAppManager.showRateAppFlow.collect {
                showRateAppDialog = it
                emitState()
            }
        }

        viewModelScope.launch {
            backupManager.allBackedUpFlowable.asFlow().collect {
                updateSettingsBadge()
            }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlow.collect {
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

        viewModelScope.launch {
            actionCompletedDelegate.walletEvents.collect { event ->
                //ContactAddedToRecent event triggered after successful send transaction
                if (event == WalletEventType.ContactAddedToRecent && donationShowManager.shouldShow()) {
                    showDonationPage()
                }
            }
        }

        updateSettingsBadge()
        updateTransactionsTabEnabled()
    }

    override fun onCleared() {
        lockGate.removeRestrictedLockListener(restrictedLockListener)
        super.onCleared()
    }

    private fun reportSelectedTab() {
        lockGate.selectedTab = selectedTabItem
    }

    override fun createState() = MainModule.UiState(
        deeplinkPage = deeplinkPage,
        mainNavItems = mainNavItems,
        showRateAppDialog = showRateAppDialog,
        showWhatsNew = showWhatsNew,
        showDonationPage = showDonationPage,
        wcSupportState = wcSupportState,
        torEnabled = torEnabled,
        openSend = openSendTokenSelect,
        selectedTabItem = selectedTabItem,
        snapTabSwitch = snapTabSwitch,
    )

    private fun isTransactionsTabEnabled(): Boolean = !accountManager.isAccountsEmpty


    fun whatsNewShown() {
        showWhatsNew = false
        emitState()
    }

    fun donationShown() {
        donationShowManager.updateDonatePageShownDate()
        showDonationPage = false
        emitState()
    }

    fun closeRateDialog() {
        showRateAppDialog = false
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
        if (items.indexOf(mainNavItem) == selectedTabIndex) {
            return
        }

        if (lockGate.isLocked && !lockGate.isTabAccessibleWhileLocked(mainNavItem)) {
            // Wallet tab tapped while browsing Market locked: ask for the PIN, then switch.
            lockGate.requireUnlocked { select(mainNavItem) }
            return
        }

        select(mainNavItem)
    }

    private fun select(mainNavItem: MainNavigation) {
        if (mainNavItem != MainNavigation.Settings) {
            currentMainTab = mainNavItem
        }
        selectTab(mainNavItem)
    }

    private fun selectTab(mainNavItem: MainNavigation) {
        val newIndex = items.indexOf(mainNavItem)
        if (newIndex < 0 || newIndex == selectedTabIndex) {
            return
        }
        // A tab switched while locked (lock fallback, widget deeplink) must not animate:
        // the outgoing wallet tab would stay composed for the crossfade's duration.
        snapTabSwitch = lockGate.isLocked
        updateSelectedTab(selectedTabIndex, newIndex)
        selectedTabIndex = newIndex
        reportSelectedTab()
        emitState()
    }

    private fun updateSelectedTab(oldIndex: Int, newIndex: Int) {
        mainNavItems = mainNavItems.toMutableList().apply {
            // Deselect old tab
            if (oldIndex in indices) {
                this[oldIndex] = this[oldIndex].copy(selected = false)
            }
            // Select new tab
            if (newIndex in indices) {
                this[newIndex] = this[newIndex].copy(selected = true)
            }
        }
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

//        MainNavigation.Transactions -> {
//            MainModule.NavigationViewItem(
//                mainNavItem = item,
//                selected = selected,
//                enabled = transactionsEnabled,
//            )
//        }

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

        MainNavigation.Swap -> {
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

        // Cold-start fallback: while DAppManager isn't yet available, WC deeplinks are routed here
        // (via MainActivity) and shown through the WC list. Once available, MainActivity pairs
        // directly so the proposal dialog works from any screen (see Nav3.handleWalletConnectDeepLink).
        val wcUri: String? = wcManager.getWalletConnectUri(deepLink)

        when {
            wcUri != null -> {
                wcSupportState = wcManager.getWalletConnectSupportState()
                if (wcSupportState == WCManager.SupportState.Supported) {
                    deeplinkPage = DeeplinkPage(WCListPage(WCListPage.Input(wcUri)))
                    tab = MainNavigation.Settings
                }
            }

            MarketDeepLinks.isMarketDeepLink(deeplinkString, deeplinkScheme) -> {
                val uid = deepLink.getQueryParameter("uid")
                when {
                    deeplinkString.contains("coin-page") -> {
                        uid?.let {
                            deeplinkPage = DeeplinkPage(CoinPage(CoinPage.Input(it)))

                            stat(page = StatPage.Widget, event = StatEvent.OpenCoin(it))
                        }
                    }

                    deeplinkString.contains("top-platforms") -> {
                        val title = deepLink.getQueryParameter("title")
                        if (title != null && uid != null) {
                            val platform = Platform(uid, title)
                            deeplinkPage = DeeplinkPage(MarketPlatformPage(platform))

                            stat(
                                page = StatPage.Widget,
                                event = StatEvent.Open(StatPage.TopPlatform)
                            )
                        }
                    }
                }

                tab = MainNavigation.Market
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
        val currentNavItem = mainNavItems.getOrNull(selectedTabIndex)?.mainNavItem
        val newIndex = currentNavItem?.let { items.indexOf(it) } ?: -1
        selectedTabIndex = if (newIndex >= 0) newIndex else items.indexOf(MainNavigation.Balance).coerceAtLeast(0)

        val newNavItems = navigationItems()

        // Only update if structure changed
        val structureChanged = mainNavItems.size != newNavItems.size ||
                mainNavItems.zip(newNavItems).any { (old, new) ->
                    old.mainNavItem != new.mainNavItem ||
                            old.enabled != new.enabled ||
                            old.badge != new.badge
                }

        if (structureChanged) {
            mainNavItems = newNavItems
            reportSelectedTab()
            emitState()
        }
    }

    private suspend fun showWhatsNew() {
        delay(2000)
        showWhatsNew = true
        emitState()
    }

    private suspend fun showDonationPage() {
        delay(2000)
        showDonationPage = true
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

    /** Dismisses a keypad shown for a deferred action and drops that action. */
    fun cancelPendingUnlockRequest() {
        lockGate.cancelUnlockRequest()
    }

    /** Deeplink that resolves to Market content only (widget taps); safe to open while locked. */
    fun isPublicDeepLink(uri: Uri): Boolean =
        lockGate.isRestricted && MarketDeepLinks.isMarketDeepLink(
            uri.toString(),
            Translator.getString(R.string.DeeplinkScheme)
        )

    fun deeplinkPageHandled() {
        deeplinkPage = null
        emitState()
    }

    fun handleDeepLink(uri: Uri) {
        val deeplinkString = uri.toString()

        // A wrapped WalletConnect link (e.g. `unstoppable.money://wc?uri=wc:...`) shares the
        // app's `unstoppable.money` scheme with TonConnect, so it would be swallowed by the
        // TonConnect branch below and the WC pairing would never start. Exclude it here and
        // let getNavigationDataForDeeplink() detect it (by `wc:` prefix or host == "wc").
        val isWalletConnectDeeplink = deeplinkString.startsWith("wc:") || uri.host == "wc"

        if (!isWalletConnectDeeplink) {
            val plugin = ChainRegistry.all.firstOrNull { it.isDeepLinkSupported(deeplinkString, fromScanner = false) }
            if (plugin != null) {
                val returnParam = uri.getQueryParameter("ret")
                // when app is opened from camera app, it returns "none" as ret param
                // so we don't need closing app in this case
                val closeApp = returnParam != "none"
                viewModelScope.launch {
                    try {
                        plugin.handleDeepLink(deeplinkString, closeApp)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A malformed link must not escape the coroutine and take the app down.
                        // This screen has no error surface, so it stays silent here, unlike the
                        // scanner path which reports it.
                        Timber.e(e, "Handling deep link failed")
                    }
                }
                return
            }
        }

        if (
            deeplinkString.startsWith("bitcoin:")
            || deeplinkString.startsWith("ethereum:")
            || deeplinkString.startsWith("toncoin:")
            || deeplinkString.startsWith("monero:")
            || deeplinkString.startsWith("tron:")
            || deeplinkString.startsWith("solana:")
            || deeplinkString.startsWith("zcash:")
            || deeplinkString.startsWith("litecoin:")
        ) {
            AddressUriParser.addressUri(deeplinkString)?.let { addressUri ->
                val allowedBlockchainTypes = addressUri.allowedBlockchainTypes
                var allowedTokenTypes: List<TokenType>? = null
                addressUri.value<String>(AddressUri.Field.TokenUid)?.let { uid ->
                    TokenType.fromId(uid)?.let { tokenType ->
                        allowedTokenTypes = listOf(tokenType)
                    }
                }

                openSendTokenSelect = OpenSendTokenSelect(
                    blockchainTypes = allowedBlockchainTypes,
                    tokenTypes = allowedTokenTypes,
                    address = addressUri.address,
                    amount = addressUri.amount
                )
                emitState()
                return
            }
        }

        val (tab, deeplinkPageData) = getNavigationDataForDeeplink(uri)
        deeplinkPage = deeplinkPageData
        currentMainTab = tab
        val newTabIndex = items.indexOf(tab)
        snapTabSwitch = lockGate.isLocked
        updateSelectedTab(selectedTabIndex, newTabIndex)
        selectedTabIndex = newTabIndex
        syncNavigation()
        reportSelectedTab()
        emitState()
    }

    fun onSendOpened() {
        openSendTokenSelect = null
        emitState()
    }

}
