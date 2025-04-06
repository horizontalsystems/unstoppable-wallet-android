package cash.p.terminal.modules.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.authorizedAction
import cash.p.terminal.core.findActivity
import cash.p.terminal.core.managers.RateAppManager
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEntity
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.modules.balance.ui.BalanceScreen
import cash.p.terminal.modules.main.MainModule.MainNavigation
import cash.p.terminal.modules.manageaccount.dialogs.BackupRequiredDialog
import cash.p.terminal.modules.market.MarketScreen
import cash.p.terminal.modules.pin.ConfirmPinFragment
import cash.p.terminal.modules.pin.PinType
import cash.p.terminal.modules.rateapp.RateApp
import cash.p.terminal.modules.releasenotes.ReleaseNotesFragment
import cash.p.terminal.modules.rooteddevice.RootedDeviceModule
import cash.p.terminal.modules.rooteddevice.RootedDeviceScreen
import cash.p.terminal.modules.rooteddevice.RootedDeviceViewModel
import cash.p.terminal.modules.settings.main.SettingsScreen
import cash.p.terminal.modules.tor.TorStatusView
import cash.p.terminal.modules.transactions.TransactionsModule
import cash.p.terminal.modules.transactions.TransactionsScreen
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.modules.walletconnect.WCAccountTypeNotSupportedDialog
import cash.p.terminal.modules.walletconnect.WCManager.SupportState
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui.compose.components.BadgeText
import cash.p.terminal.ui.compose.components.HsBottomNavigation
import cash.p.terminal.ui.compose.components.HsBottomNavigationItem
import cash.p.terminal.ui.extensions.WalletSwitchBottomSheet
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainFragment : BaseComposeFragment() {

    private var transactionsViewModelRef: WeakReference<TransactionsViewModel>? = null

    @Composable
    override fun GetContent(navController: NavController) {
        val backStackEntry = navController.safeGetBackStackEntry(R.id.mainFragment)

        backStackEntry?.let {
            val viewModel = ViewModelProvider(
                backStackEntry.viewModelStore,
                TransactionsModule.Factory()
            )[TransactionsViewModel::class.java]
            transactionsViewModelRef = WeakReference(viewModel)
            MainScreenWithRootedDeviceCheck(
                transactionsViewModel = viewModel,
                navController = navController,
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

    override fun onPause() {
        super.onPause()
        transactionsViewModelRef?.get()?.showAllTransactions(false)
    }

}

@Composable
private fun MainScreenWithRootedDeviceCheck(
    transactionsViewModel: TransactionsViewModel,
    navController: NavController,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory())
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(transactionsViewModel, navController)
    }
}

@Composable
private fun MainScreen(
    transactionsViewModel: TransactionsViewModel,
    fragmentNavController: NavController,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory())
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    val selectedPage = uiState.selectedTabIndex
    val pagerState = rememberPagerState(initialPage = selectedPage) { uiState.mainNavItems.size }

    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    LaunchedEffect(Unit) {
        context.findActivity()?.let { activity ->
            activity.intent?.data?.let { uri ->
                viewModel.handleDeepLink(uri)
                activity.intent?.data = null //clear intent data
            }
        }
    }
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
            containerColor = ComposeAppTheme.colors.tyler,
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
                                },
                                onLongClick = {
                                    if (item.mainNavItem == MainNavigation.Balance) {
                                        coroutineScope.launch {
                                            modalBottomSheetState.show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            BackHandler(enabled = modalBottomSheetState.isVisible) {
                coroutineScope.launch {
                    modalBottomSheetState.hide()
                }
            }
            Column {
                LaunchedEffect(key1 = selectedPage, block = {
                    if (uiState.mainNavItems[selectedPage].mainNavItem != MainNavigation.Transactions) {
                        transactionsViewModel.showAllTransactions(false)
                    }
                    pagerState.scrollToPage(selectedPage)
                })

                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState,
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (uiState.mainNavItems[page].mainNavItem) {
                        MainNavigation.Market -> MarketScreen(fragmentNavController, paddingValues)
                        MainNavigation.Balance -> BalanceScreen(fragmentNavController, paddingValues)
                        MainNavigation.Transactions -> TransactionsScreen(
                            navController = fragmentNavController,
                            paddingValues = paddingValues,
                            viewModel = transactionsViewModel,
                            onShowAllTransactionsClicked = {
                                fragmentNavController.authorizedAction(
                                    ConfirmPinFragment.InputConfirm(
                                        descriptionResId = R.string.Unlock_EnterPasscode_Transactions_Hide,
                                        pinType = PinType.TRANSACTIONS_HIDE
                                    )
                                ) {
                                    transactionsViewModel.showAllTransactions(true)
                                }
                            }
                        )

                        MainNavigation.Settings -> SettingsScreen(fragmentNavController, paddingValues)
                    }
                }
            }
        }
        HideContentBox(uiState.contentHidden)
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
private fun HideContentBox(contentHidden: Boolean) {
    val backgroundModifier = if (contentHidden) {
        Modifier.background(ComposeAppTheme.colors.tyler)
    } else {
        Modifier
    }
    Box(
        Modifier
            .fillMaxSize()
            .then(backgroundModifier)
    )
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