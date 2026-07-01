package io.horizontalsystems.bankwallet.modules.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.multiswap.SwapScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.rateapp.RateApp
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesPage
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectPage
import io.horizontalsystems.bankwallet.modules.settings.donate.WhyDonatePage
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.bankwallet.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItem
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItemDefaults
import kotlinx.coroutines.delay

@Composable
fun MainScreenWithRootedDeviceCheck(
    transactionsViewModel: TransactionsViewModel,
    navigation: HSNavigation,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    mainActivityViewModel: MainActivityViewModel,
    parentScreenContentKey: String
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(mainActivityViewModel, transactionsViewModel, navigation, parentScreenContentKey)
    }
}

@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    transactionsViewModel: TransactionsViewModel,
    navigation: HSNavigation,
    parentScreenContentKey: String,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory())
) {
    val activityIntent by mainActivityViewModel.intentLiveData.observeAsState()
    LaunchedEffect(activityIntent) {
        activityIntent?.data?.let {
            delay(1000)
            viewModel.handleDeepLink(it)
            mainActivityViewModel.intentHandled()
        }
    }

    val uiState = viewModel.uiState
    val navigationBarHeight = 56.dp

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(ComposeAppTheme.colors.blade)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (uiState.torEnabled) {
                    TorStatusView()
                }
                NavigationBar(
                    modifier = Modifier.height(navigationBarHeight),
                    containerColor = ComposeAppTheme.colors.blade,
                ) {
                    uiState.mainNavItems.forEach { destination ->
                        HsNavigationBarItem(
                            selected = destination.selected,
                            onClick = {
                                viewModel.onSelect(destination.mainNavItem)
                                stat(
                                    page = StatPage.Main,
                                    event = StatEvent.SwitchTab(destination.mainNavItem.statTab)
                                )
                            },
                            onLongClick = if (destination.selected && destination.mainNavItem == MainNavigation.Balance) {
                                {
                                    navigation.slideFromBottom(WalletSwitchSheet)
                                    stat(
                                        page = StatPage.Main,
                                        event = StatEvent.Open(StatPage.SwitchWallet)
                                    )
                                }
                            } else null,
                            enabled = destination.enabled,
                            colors = HsNavigationBarItemDefaults.colors(
                                selectedIconColor = ComposeAppTheme.colors.jacob,
                                unselectedIconColor = ComposeAppTheme.colors.grey,
                                indicatorColor = ComposeAppTheme.colors.transparent,
                                selectedTextColor = ComposeAppTheme.colors.jacob,
                                unselectedTextColor = ComposeAppTheme.colors.grey,
                            ),
                            icon = {
                                BadgedIcon(destination.badge) {
                                    Icon(
                                        painter = painterResource(destination.mainNavItem.iconRes),
                                        contentDescription = stringResource(destination.mainNavItem.titleRes)
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column {
            Crossfade(uiState.selectedTabItem) { navItem ->
                when (navItem) {
                    MainNavigation.Market -> MarketScreen(navigation)
                    MainNavigation.Balance -> BalanceScreen(navigation)
                    MainNavigation.Swap -> SwapScreen(
                        navigation = navigation,
                        parentScreenContentKey = parentScreenContentKey,
                        onClickClose = null,
                        bottomPadding = navigationBarHeight,
                        closeAfterSwap = false,
                        autofocus = false
                    )

                    MainNavigation.Settings -> SettingsScreen(navigation)
                }
            }
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            navigation.slideFromBottom(
                ReleaseNotesPage(ReleaseNotesPage.Input(true))
            )
            viewModel.whatsNewShown()
        }
    }

    if (uiState.showDonationPage) {
        LaunchedEffect(Unit) {
            navigation.slideFromBottom(WhyDonatePage)
            viewModel.donationShown()
        }
    }

    if (uiState.showRateAppDialog) {
        val context = LocalContext.current
        RateApp(
            onRateClick = {
                RateAppManager.openPlayMarket(context)
                viewModel.closeRateDialog()
            },
            onCancelClick = { viewModel.closeRateDialog() }
        )
    }

    if (uiState.wcSupportState != null) {
        when (val wcSupportState = uiState.wcSupportState) {
            SupportState.NotSupportedDueToNoActiveAccount -> {
                navigation.slideFromBottom(WCErrorNoAccountSheet)
            }

            is SupportState.NotSupported -> {
                navigation.slideFromBottom(
                    WCAccountTypeNotSupportedSheet(WCAccountTypeNotSupportedSheet.Input(wcSupportState.accountTypeDescription))
                )
            }

            else -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    uiState.deeplinkPage?.let { deepLinkPage ->
        LaunchedEffect(Unit) {
            delay(500)
            navigation.slideFromRight(deepLinkPage.screen)
            viewModel.deeplinkPageHandled()
        }
    }

    uiState.openSend?.let { openSend ->
        navigation.slideFromRight(
            SendTokenSelectPage(SendTokenSelectPage.Input(
                openSend.blockchainTypes,
                openSend.tokenTypes,
                openSend.address,
                openSend.amount,
                openSend.memo,
            ))
        )
        viewModel.onSendOpened()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
        mainActivityViewModel.reEmitPendingWcEventIfNeeded()
    }
}

@Composable
private fun BadgedIcon(
    badge: MainModule.BadgeType?,
    icon: @Composable BoxScope.() -> Unit,
) {
    when (badge) {
        is MainModule.BadgeType.BadgeNumber ->
            BadgedBox(
                badge = {
                    BadgeText(
                        text = badge.number.toString(),
                    )
                },
                content = icon
            )

        MainModule.BadgeType.BadgeDot ->
            BadgedBox(
                badge = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = 7.dp, y = (-9).dp)
                            .background(
                                ComposeAppTheme.colors.lucian,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) { }
                },
                content = icon
            )

        else -> {
            Box {
                icon()
            }
        }
    }
}
