package cash.p.terminal.modules.main

import android.net.Uri
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.IBackupManager
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IRateAppManager
import cash.p.terminal.core.ITermsManager
import cash.p.terminal.core.managers.ReleaseNotesManager
import cash.p.terminal.core.managers.isTonConnectDeeplink
import cash.p.terminal.core.usecase.CheckGooglePlayUpdateUseCase
import cash.p.terminal.core.usecase.UpdateResult
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.entities.AddressUri
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.feature.logging.domain.usecase.LogLoginAttemptUseCase
import cash.p.terminal.modules.balance.OpenSendTokenSelect
import cash.p.terminal.modules.main.MainModule.MainNavigation
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.modules.nft.collection.NftCollectionFragment
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import cash.p.terminal.modules.walletconnect.list.WCListFragment
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.ActiveAccountState
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.core.deeplink.DeeplinkParser
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.koin.java.KoinJavaComponent.inject

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
    private val logLoginAttemptUseCase: LogLoginAttemptUseCase,
    private val deeplinkParser: DeeplinkParser
) : ViewModelUiState<MainModule.UiState>() {

    private val checkGooglePlayUpdateUseCase: CheckGooglePlayUpdateUseCase by inject(
        CheckGooglePlayUpdateUseCase::class.java
    )

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
    private var contentHidden = pinComponent.isPinSet
    private var showWhatsNew = false
    private var activeWallet = accountManager.activeAccount
    private var wcSupportState: WCManager.SupportState? = null
    private var torEnabled = localStorage.torEnabled
    private var openSendTokenSelect: OpenSendTokenSelect? = null
    private val updateAvailable: StateFlow<Boolean> = checkGooglePlayUpdateUseCase()
        .map { it is UpdateResult.ImmediateUpdateAvailable || it is UpdateResult.FlexibleUpdateAvailable }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val wallets: List<Account>
        get() = accountManager.accounts.filter { !it.isWatchAccount }

    val watchWallets: List<Account>
        get() = accountManager.accounts.filter { it.isWatchAccount }

    init {
        pinComponent.isLockedFlow.collectWith(viewModelScope) {
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
            backupManager.allBackedUpFlow.collect {
                updateSettingsBadge()
            }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                updateSettingsBadge()
            }
        }
        viewModelScope.launch {
            accountManager.accountsFlow.collect {
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

        updateAvailable.collectWith(viewModelScope) {
            if (it) {
                updateSettingsBadge()
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
        torEnabled = torEnabled,
        openSend = openSendTokenSelect,
    )

    private fun isTransactionsTabEnabled(): Boolean =
        !accountManager.isAccountsEmpty


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
        // Check if we need to switch to Balance tab
        if (localStorage.selectBalanceTabOnNextLaunch) {
            localStorage.selectBalanceTabOnNextLaunch = false
            onSelect(MainNavigation.Balance)
        }

        emitState()
        viewModelScope.launch {
            if (!pinComponent.isLockedFlow.value && releaseNotesManager.shouldShowChangeLog()) {
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

        // Try parsing with shared DeeplinkParser first (handles premium and swap)
        deeplinkParser.parse(deepLink)?.let { parsedPage ->
            return MainNavigation.Balance to parsedPage
        }

        val deeplinkScheme: String =
            cash.p.terminal.strings.helpers.Translator.getString(R.string.DeeplinkScheme)
        when {
            deeplinkString.startsWith("$deeplinkScheme:") -> {
                val uid = deepLink.getQueryParameterSafe("uid")
                when {
                    deeplinkString.contains("coin-page") -> {
                        uid?.let {
                            deeplinkPage = DeeplinkPage(R.id.coinFragment, CoinFragmentInput(it))
                            tab = MainNavigation.Market
                        }
                    }

                    deeplinkString.contains("nft-collection") -> {
                        val blockchainTypeUid = deepLink.getQueryParameterSafe("blockchainTypeUid")
                        if (uid != null && blockchainTypeUid != null) {
                            deeplinkPage = DeeplinkPage(
                                R.id.nftCollectionFragment,
                                NftCollectionFragment.Input(uid, blockchainTypeUid)
                            )
                            tab = MainNavigation.Market
                        }
                    }

                    deeplinkString.contains("top-platforms") -> {
                        val title = deepLink.getQueryParameterSafe("title")
                        if (title != null && uid != null) {
                            val platform = Platform(uid, title)
                            deeplinkPage = DeeplinkPage(R.id.marketPlatformFragment, platform)
                            tab = MainNavigation.Market
                        }
                    }
                }
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
            !(backupManager.allBackedUp && termsManager.allTermsAccepted && pinComponent.isPinSet) ||
                    accountManager.hasNonStandardAccount ||
                    updateAvailable.value ||
                    !localStorage.isSystemPinRequired

        settingsBadge = if (wcPendingRequestsCount > 0) {
            MainModule.BadgeType.BadgeNumber(wcPendingRequestsCount)
        } else if (showDotBadge) {
            MainModule.BadgeType.BadgeDot
        } else {
            null
        }
        syncNavigation()
        checkSelfieProblem()
    }

    private fun checkSelfieProblem() {
        if (settingsBadge != null) {
            return
        }
        viewModelScope.launch {
            if (logLoginAttemptUseCase.selfieEnabledAndHasProblem()) {
                settingsBadge = MainModule.BadgeType.BadgeDot
                syncNavigation()
            }
        }
    }

    fun deeplinkPageHandled() {
        deeplinkPage = null
        emitState()
    }

    fun handleDeepLink(uri: Uri) {
        if (uri.isTonConnectDeeplink()) {
            val returnParam = uri.getQueryParameterSafe("ret")
            // when app is opened from camera app, it returns "none" as ret param
            // so we don't need closing app in this case
            val closeApp = returnParam != "none"
            viewModelScope.launch {
                App.tonConnectManager.handle(uri.toString(), closeApp)
            }
            return
        }

        val deeplinkString = uri.toString()
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

/**
 * Safely extracts a query parameter from both hierarchical and opaque URIs.
 *
 * Hierarchical: `pcash://coin-page?uid=bitcoin`
 * Opaque: `pcash:coin-page?uid=bitcoin`
 *
 * Returns null for parameters without `=` (e.g., `?uid`), matching Android's behavior.
 * Returns empty string for parameters with empty value (e.g., `?uid=`).
 */
private fun Uri.getQueryParameterSafe(key: String): String? {
    return if (isHierarchical) {
        getQueryParameter(key)
    } else {
        // For opaque URIs, parse schemeSpecificPart manually
        // e.g., "coin-page?uid=bitcoin&foo=bar"
        val ssp = schemeSpecificPart ?: return null
        val queryStart = ssp.indexOf('?')
        if (queryStart == -1) return null

        val queryString = ssp.substring(queryStart + 1)
        queryString.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }
}
