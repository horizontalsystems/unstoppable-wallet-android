package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceScreen
import io.horizontalsystems.bankwallet.modules.main.MainModule.MainNavigation
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.market.MarketScreen
import io.horizontalsystems.bankwallet.modules.rateapp.RateApp
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceModule
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceScreen
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceViewModel
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsScreen
import io.horizontalsystems.bankwallet.modules.tor.TorStatusView
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigation
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigationItem
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainFragment : BaseComposeFragment() {
    private val mainActivityViewModel by activityViewModels<MainActivityViewModel>()

    @Composable
    override fun GetContent(navController: NavController) {
        val backStackEntry = navController.safeGetBackStackEntry(R.id.mainFragment)

        backStackEntry?.let {
            val viewModel = ViewModelProvider(backStackEntry.viewModelStore,  TransactionsModule.Factory())
                .get(TransactionsViewModel::class.java)
            MainScreenWithRootedDeviceCheck(
                transactionsViewModel = viewModel,
                navController = navController,
                mainActivityViewModel = mainActivityViewModel
            )
        } ?: run {
            // Back stack entry doesn't exist, restart activity
            val intent = Intent(context, MainActivity::class.java)
            requireActivity().startActivity(intent)
        }
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
    transactionsViewModel: TransactionsViewModel,
    navController: NavController,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory()),
    mainActivityViewModel: MainActivityViewModel
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(mainActivityViewModel, transactionsViewModel, navController)
    }
}

@Composable
private fun MainScreen(
    mainActivityViewModel: MainActivityViewModel,
    transactionsViewModel: TransactionsViewModel,
    fragmentNavController: NavController,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory())
) {
    val activityIntent by mainActivityViewModel.intentLiveData.observeAsState()
    LaunchedEffect(activityIntent) {
        activityIntent?.data?.let {
            mainActivityViewModel.intentHandled()
            viewModel.handleDeepLink(it)
        }
    }

    val uiState = viewModel.uiState
    val selectedPage = uiState.selectedTabIndex
    val pagerState = rememberPagerState(initialPage = selectedPage) { uiState.mainNavItems.size }

    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            WalletSwitchBottomSheet(
                wallets = viewModel.wallets,
                watchingAddresses = viewModel.watchWallets,
                selectedAccount = uiState.activeWallet,
                onSelectListener = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                        viewModel.onSelect(it)

                        stat(page = StatPage.SwitchWallet, event = StatEvent.Select(StatEntity.Wallet))
                    }
                },
                onCancelClick = {
                    coroutineScope.launch {
                        modalBottomSheetState.hide()
                    }
                }
            )
        },
    ) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            bottomBar = {
                Column {
                    if (uiState.torEnabled) {
                        TorStatusView()
                    }
                    HsBottomNavigation(
                        backgroundColor = ComposeAppTheme.colors.tyler,
                        elevation = 10.dp
                    ) {
                        uiState.mainNavItems.forEach { item ->
                            HsBottomNavigationItem(
                                icon = {
                                    BadgedIcon(item.badge) {
                                        Icon(
                                            painter = painterResource(item.mainNavItem.iconRes),
                                            contentDescription = stringResource(item.mainNavItem.titleRes)
                                        )
                                    }
                                },
                                selected = item.selected,
                                enabled = item.enabled,
                                selectedContentColor = ComposeAppTheme.colors.jacob,
                                unselectedContentColor = if (item.enabled) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.grey50,
                                onClick = {
                                    viewModel.onSelect(item.mainNavItem)

                                    stat(
                                        page = StatPage.Main,
                                        event = StatEvent.SwitchTab(item.mainNavItem.statTab)
                                    )
                                },
                                onLongClick = {
                                    if (item.mainNavItem == MainNavigation.Balance) {
                                        coroutineScope.launch {
                                            modalBottomSheetState.show()

                                            stat(
                                                page = StatPage.Main,
                                                event = StatEvent.Open(StatPage.SwitchWallet)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) {
            BackHandler(enabled = modalBottomSheetState.isVisible) {
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                }
            }
            Column(modifier = Modifier.padding(it)) {
                LaunchedEffect(key1 = selectedPage, block = {
                    pagerState.scrollToPage(selectedPage)
                })

                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState,
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (uiState.mainNavItems[page].mainNavItem) {
                        MainNavigation.Market -> MarketScreen(fragmentNavController)
                        MainNavigation.Balance -> BalanceScreen(fragmentNavController)
                        MainNavigation.Transactions -> TransactionsScreen(
                            fragmentNavController,
                            transactionsViewModel
                        )

                        MainNavigation.Settings -> SettingsScreen(fragmentNavController)
                    }
                }
            }
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(
                R.id.releaseNotesFragment,
                ReleaseNotesFragment.Input(true)
            )
            viewModel.whatsNewShown()
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
                fragmentNavController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            }

            is SupportState.NotSupportedDueToNonBackedUpAccount -> {
                val text = stringResource(R.string.WalletConnect_Error_NeedBackup)
                fragmentNavController.slideFromBottom(
                    R.id.backupRequiredDialog,
                    BackupRequiredDialog.Input(wcSupportState.account, text)
                )

                stat(page = StatPage.Main, event = StatEvent.Open(StatPage.BackupRequired))
            }

            is SupportState.NotSupported -> {
                fragmentNavController.slideFromBottom(
                    R.id.wcAccountTypeNotSupportedDialog,
                    WCAccountTypeNotSupportedDialog.Input(wcSupportState.accountTypeDescription)
                )
            }

            else -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    uiState.deeplinkPage?.let { deepLinkPage ->
        LaunchedEffect(Unit) {
            delay(500)
            fragmentNavController.slideFromRight(
                deepLinkPage.navigationId,
                deepLinkPage.input
            )
            viewModel.deeplinkPageHandled()
        }
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

fun NavController.safeGetBackStackEntry(destinationId: Int): NavBackStackEntry? {
    return try {
        this.getBackStackEntry(destinationId)
    } catch (e: IllegalArgumentException) {
        null
    }
}