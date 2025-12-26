package io.horizontalsystems.bankwallet.modules.main

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ActionCompletedDelegate
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.managers.DonationShowManager
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.bankwallet.core.managers.WalletEventType
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AddressUri
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.balance.OpenSendTokenSelect
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.market.topplatforms.Platform
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WCSessionManager
import io.horizontalsystems.bankwallet.modules.walletconnect.list.WCListFragment
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.marketkit.models.TokenType
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
    private val donationShowManager: DonationShowManager,
    private val localStorage: ILocalStorage,
    wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    private val networkManager: INetworkManager,
    private val actionCompletedDelegate: ActionCompletedDelegate
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
    private var showDonationPage = false
    private var activeWallet = accountManager.activeAccount
    private var wcSupportState: WCManager.SupportState? = null
    private var torEnabled = localStorage.torEnabled
    private var openSendTokenSelect: OpenSendTokenSelect? = null

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    init {
        localStorage.marketsTabEnabledFlow.collectWith(viewModelScope) {
            marketsTabEnabled = it
            syncNavigation()
        }

        termsManager.termsAcceptedSharedFlow.collectWith(viewModelScope) {
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

        accountManager.activeAccountStateFlow.collectWith(viewModelScope) {
            (it as? ActiveAccountState.ActiveAccount)?.let { state ->
                activeWallet = state.account
                emitState()
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

    override fun createState() = MainModule.UiState(
        selectedTabIndex = selectedTabIndex,
        deeplinkPage = deeplinkPage,
        mainNavItems = mainNavItems,
        showRateAppDialog = showRateAppDialog,
        showWhatsNew = showWhatsNew,
        showDonationPage = showDonationPage,
        activeWallet = activeWallet,
        wcSupportState = wcSupportState,
        torEnabled = torEnabled,
        openSend = openSendTokenSelect,
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
                    deeplinkPage = DeeplinkPage(R.id.wcListFragment, WCListFragment.Input(deeplinkString))
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

    fun deeplinkPageHandled() {
        deeplinkPage = null
        emitState()
    }

    fun handleDeepLink(uri: Uri) {
        val deeplinkString = uri.toString()
        if (deeplinkString.startsWith("unstoppable.money:") || deeplinkString.startsWith("tc:")) {
            val returnParam = uri.getQueryParameter("ret")
            // when app is opened from camera app, it returns "none" as ret param
            // so we don't need closing app in this case
            val closeApp = returnParam != "none"
            viewModelScope.launch {
                App.tonConnectManager.handle(uri.toString(), closeApp)
            }
            return
        }

        if (
            deeplinkString.startsWith("bitcoin:")
            || deeplinkString.startsWith("ethereum:")
            || deeplinkString.startsWith("toncoin:")
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
        selectedTabIndex = items.indexOf(tab)
        syncNavigation()
    }

    fun onSendOpened() {
        openSendTokenSelect = null
        emitState()
    }

}
