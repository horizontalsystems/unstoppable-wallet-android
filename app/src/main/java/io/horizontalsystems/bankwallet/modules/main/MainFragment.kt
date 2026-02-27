package io.horizontalsystems.bankwallet.modules.main

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredScreen
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.NavExample
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.rateapp.RateApp
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectScreen
import io.horizontalsystems.bankwallet.modules.settings.donate.WhyDonateScreen
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.WCErrorNoAccountScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItem
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.HsNavigationBarItemDefaults
import kotlinx.serialization.Serializable

class MainFragment : BaseComposeFragment() {
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        NavExample(mainActivityViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            })
    }

}

@Composable
private fun MainScreenWithRootedDeviceCheck(
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    mainActivityViewModel: MainActivityViewModel,
    backStack: NavBackStack<HSScreen>,
    resultBus: ResultEventBus
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(
            mainActivityViewModel = mainActivityViewModel,
            backStack = backStack,
            resultBus = resultBus
        )
    }
}

@Serializable
data object MainScreen : HSScreen() {
    // TODO("Nav3: need to find other solution. There should not be mainActivityViewModel")
    lateinit var mainActivityViewModel: MainActivityViewModel

    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        MainScreenWithRootedDeviceCheck(
            mainActivityViewModel = mainActivityViewModel,
            backStack = backStack,
            resultBus = resultBus,
        )
    }
}

@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    backStack: NavBackStack<HSScreen>,
    resultBus: ResultEventBus
) {
    val viewModel = viewModel<MainViewModel>(factory = MainModule.Factory())
    val activityIntent by mainActivityViewModel.intentLiveData.observeAsState()
    LaunchedEffect(activityIntent) {
        activityIntent?.data?.let {
            mainActivityViewModel.intentHandled()
            viewModel.handleDeepLink(it)
        }
    }

    val uiState = viewModel.uiState
    val navigationBarHeight = 56.dp

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
                                    backStack.add(WalletSwitchScreen)
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
    ) {
        Column {
            Crossfade(uiState.selectedTabItem) { navItem ->
                when (navItem) {
                    MainNavigation.Market -> MarketScreen(backStack, resultBus)
                    MainNavigation.Balance -> BalanceScreen(backStack)
                    MainNavigation.Swap -> {
//                        TODO("xxx nav3")
//                        SwapScreen(
//                            fragmentNavController,
//                            tokenIn = null,
//                            onClickClose = null,
//                            bottomPadding = navigationBarHeight,
//                            closeAfterSwap = false
//                        )
                    }
                    MainNavigation.Transactions -> TransactionsScreen(backStack)
                    MainNavigation.Settings -> SettingsScreen(backStack)
                }
            }
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            backStack.add(ReleaseNotesScreen(true))
            viewModel.whatsNewShown()
        }
    }

    if (uiState.showDonationPage) {
        LaunchedEffect(Unit) {
            backStack.add(WhyDonateScreen)
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
                backStack.add(WCErrorNoAccountScreen)
            }

            is SupportState.NotSupportedDueToNonBackedUpAccount -> {
                val text = stringResource(R.string.WalletConnect_Error_NeedBackup)
                backStack.add(BackupRequiredScreen(wcSupportState.account, text))
                stat(page = StatPage.Main, event = StatEvent.Open(StatPage.BackupRequired))
            }

            is SupportState.NotSupported -> {
                backStack.add(WCAccountTypeNotSupportedScreen(wcSupportState.accountTypeDescription))
            }

            else -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    uiState.deeplinkPage?.let { deepLinkPage ->
        LaunchedEffect(Unit) {
//            TODO("xxx nav3")
//            delay(500)
//            fragmentNavController.slideFromRight(
//                deepLinkPage.navigationId,
//                deepLinkPage.input
//            )
//            viewModel.deeplinkPageHandled()
        }
    }

    uiState.openSend?.let { openSend ->
        backStack.add(SendTokenSelectScreen(
            openSend.blockchainTypes,
            openSend.tokenTypes,
            openSend.address,
            openSend.amount,
            openSend.memo,
        ))
        viewModel.onSendOpened()
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
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
