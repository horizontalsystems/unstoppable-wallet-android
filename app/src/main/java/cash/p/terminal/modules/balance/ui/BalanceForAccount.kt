package cash.p.terminal.modules.balance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.MainGraphDirections
import cash.p.terminal.R
import cash.p.terminal.core.Caution
import cash.p.terminal.navigation.openQrScanner
import cash.p.terminal.modules.backupalert.BackupAlert
import cash.p.terminal.modules.balance.AccountViewItem
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.contacts.screen.ConfirmationBottomSheet
import cash.p.terminal.modules.manageaccounts.ManageAccountsModule
import cash.p.terminal.modules.walletconnect.list.WalletConnectListViewModel
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.ui_compose.rememberDebouncedAction
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BalanceForAccount(
    navController: NavController,
    accountViewItem: AccountViewItem,
    paddingValuesParent: PaddingValues
) {
    val viewModel = viewModel<BalanceViewModel>(factory = BalanceModule.Factory())

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    val invalidUrlBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val scannerTitle = stringResource(R.string.qr_scanner_title_smart_scan)

    viewModel.uiState.errorMessage?.let { message ->
        val view = LocalView.current
        HudHelper.showErrorMessage(view, text = message)
        viewModel.errorShown()
    }

    when (viewModel.connectionResult) {
        WalletConnectListViewModel.ConnectionResult.Error -> {
            LaunchedEffect(viewModel.connectionResult) {
                coroutineScope.launch {
                    delay(300)
                    invalidUrlBottomSheetState.show()
                }
            }
            viewModel.onHandleRoute()
        }

        else -> Unit
    }

    BackupAlert(navController)
    ModalBottomSheetLayout(
        sheetState = invalidUrlBottomSheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            ConfirmationBottomSheet(
                title = stringResource(R.string.WalletConnect_Title),
                text = stringResource(R.string.WalletConnect_Error_InvalidUrl),
                iconPainter = painterResource(R.drawable.ic_wallet_connect_24),
                iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                confirmText = stringResource(R.string.Button_TryAgain),
                cautionType = Caution.Type.Warning,
                cancelText = stringResource(R.string.Button_Cancel),
                onConfirm = {
                    coroutineScope.launch {
                        invalidUrlBottomSheetState.hide()
                        navController.openQrScanner(
                            title = scannerTitle,
                            showPasteButton = true
                        ) { scannedText ->
                            viewModel.handleScannedData(scannedText)
                        }
                    }
                },
                onClose = {
                    coroutineScope.launch { invalidUrlBottomSheetState.hide() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = {
                        BalanceTitleRow(navController, accountViewItem.name)
                    },
                    menuItems = buildList {
                        if (accountViewItem.isCoinManagerEnabled) {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.display_options),
                                    icon = R.drawable.ic_search,
                                    onClick = {
                                        navController.slideFromRight(R.id.manageWalletsFragment)
                                    })
                            )
                        }
                        if (!accountViewItem.type.isWatchAccountType) {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.WalletConnect_NewConnect),
                                    icon = R.drawable.ic_qr_scan_20,
                                    onClick = {
                                        navController.openQrScanner(
                                            title = scannerTitle,
                                            showPasteButton = true
                                        ) { scannedText ->
                                            viewModel.handleScannedData(scannedText)
                                        }
                                    }
                                )
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            val uiState = viewModel.uiState

            val navigateToTokenBalance: (BalanceViewItem2) -> Unit = rememberDebouncedAction { item ->
                navController.navigate(
                    MainGraphDirections.actionToTokenBalance(item.wallet)
                )
            }

            Crossfade(
                targetState = uiState.viewState,
                modifier = Modifier
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValuesParent.calculateBottomPadding()
                    )
                    .fillMaxSize(),
                label = ""
            ) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
                        val balanceViewItems = uiState.balanceViewItems
                        BalanceItems(
                            balanceViewItems = balanceViewItems,
                            viewModel = viewModel,
                            onItemClick = navigateToTokenBalance,
                            onBalanceClick = viewModel::onBalanceClick,
                            accountViewItem = accountViewItem,
                            navController = navController,
                            uiState = uiState,
                            totalState = viewModel.totalUiState,
                        )
                    }

                    ViewState.Loading,
                    is ViewState.Error,
                    null -> {
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceTitleRow(
    navController: NavController,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        title3_leah(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_down_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.yellowD,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    navController.slideFromBottom(
                        R.id.manageAccountsFragment,
                        ManageAccountsModule.Mode.Switcher
                    )
                },
        )
    }
}
