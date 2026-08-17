package io.horizontalsystems.walletkit.modules.main

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.RateAppManager
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statTab
import io.horizontalsystems.walletkit.modules.balance.ui.BalanceScreen
import io.horizontalsystems.walletkit.modules.main.MainModule.MainNavigation
import io.horizontalsystems.walletkit.modules.market.MarketScreen
import io.horizontalsystems.walletkit.modules.multiswap.SwapScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.rateapp.RateApp
import io.horizontalsystems.walletkit.modules.releasenotes.ReleaseNotesPage
import io.horizontalsystems.walletkit.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.walletkit.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.walletkit.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.walletkit.modules.sendtokenselect.SendTokenSelectPage
import io.horizontalsystems.walletkit.modules.settings.donate.WhyDonatePage
import io.horizontalsystems.walletkit.modules.settings.main.SettingsScreen
import io.horizontalsystems.walletkit.modules.tor.TorStatusView
import io.horizontalsystems.walletkit.modules.transactions.TransactionsViewModel
import io.horizontalsystems.walletkit.modules.walletconnect.WCAccountTypeNotSupportedSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCErrorNoAccountSheet
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.BadgeText
import io.horizontalsystems.walletkit.uiv3.components.bottombars.HsNavigationBarItem
import io.horizontalsystems.walletkit.uiv3.components.bottombars.HsNavigationBarItemDefaults
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
    val isLocked by App.pinComponent.isLockedFlow.collectAsStateWithLifecycle()
    // MainScreen stays composed under the unlock overlay, so without this gate a deeplink opened
    // while the app is locked would be acted on behind the lock screen — referral registration,
    // TonConnect links, prefilled send flows, the WalletConnect list. The intent is deliberately
    // left unconsumed (no intentHandled() call) so this effect re-runs when isLocked flips back.
    LaunchedEffect(activityIntent, isLocked) {
        if (isLocked) return@LaunchedEffect

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
