package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
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
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager.SupportState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigation
import io.horizontalsystems.bankwallet.ui.compose.components.HsBottomNavigationItem
import io.horizontalsystems.bankwallet.ui.extensions.WalletSwitchBottomSheet
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class MainFragment : BaseFragment() {

    private val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    MainScreenWithRootedDeviceCheck(
                        transactionsViewModel = transactionsViewModel,
                        deepLink = activity?.intent?.data?.toString(),
                        navController = findNavController(),
                        clearActivityData = { activity?.intent?.data = null }
                    )
                }
            }
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
    deepLink: String?,
    navController: NavController,
    clearActivityData: () -> Unit,
    rootedDeviceViewModel: RootedDeviceViewModel = viewModel(factory = RootedDeviceModule.Factory())
) {
    if (rootedDeviceViewModel.showRootedDeviceWarning) {
        RootedDeviceScreen { rootedDeviceViewModel.ignoreRootedDeviceWarning() }
    } else {
        MainScreen(transactionsViewModel, deepLink, navController, clearActivityData)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    transactionsViewModel: TransactionsViewModel,
    deepLink: String?,
    fragmentNavController: NavController,
    clearActivityData: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainModule.Factory(deepLink))
) {

    val uiState = viewModel.uiState
    val selectedPage = uiState.selectedPageIndex
    val pagerState = rememberPagerState(initialPage = selectedPage)

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
        Box(Modifier.fillMaxSize()) {
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
                                    onClick = { viewModel.onSelect(item.mainNavItem) },
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
                        pageCount = uiState.mainNavItems.size,
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
            HideContentBox(uiState.contentHidden)
        }
    }

    if (uiState.showWhatsNew) {
        LaunchedEffect(Unit) {
            fragmentNavController.slideFromBottom(
                R.id.releaseNotesFragment,
                bundleOf(ReleaseNotesFragment.showAsClosablePopupKey to true)
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
            SupportState.Supported -> {
                fragmentNavController.slideFromRight(R.id.wallet_connect_graph)
            }
            SupportState.NotSupportedDueToNoActiveAccount -> {
                clearActivityData.invoke()
                fragmentNavController.slideFromBottom(R.id.wcErrorNoAccountFragment)
            }
            is SupportState.NotSupportedDueToNonBackedUpAccount -> {
                clearActivityData.invoke()
                val text = stringResource(
                    R.string.WalletConnect_Error_NeedBackup,
                    wcSupportState.account.name
                )
                fragmentNavController.slideFromBottom(
                    R.id.backupRequiredDialog,
                    BackupRequiredDialog.prepareParams(wcSupportState.account, text)
                )
            }
            is SupportState.NotSupported -> {
                clearActivityData.invoke()
                fragmentNavController.slideFromBottom(
                    R.id.wcAccountTypeNotSupportedDialog,
                    WCAccountTypeNotSupportedDialog.prepareParams(wcSupportState.accountTypeDescription)
                )
            }
            null -> {}
        }
        viewModel.wcSupportStateHandled()
    }

    DisposableLifecycleCallbacks(
        onResume = viewModel::onResume,
    )
}

@Composable
private fun HideContentBox(contentHidden: Boolean) {
    val backgroundModifier = if (contentHidden) {
        Modifier.background(ComposeAppTheme.colors.tyler)
    } else {
        Modifier
    }
    Box(Modifier.fillMaxSize().then(backgroundModifier))
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
                    Badge(
                        backgroundColor = ComposeAppTheme.colors.lucian
                    ) {
                        Text(
                            text = badge.number.toString(),
                            style = ComposeAppTheme.typography.micro,
                            color = ComposeAppTheme.colors.white,
                        )
                    }
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
