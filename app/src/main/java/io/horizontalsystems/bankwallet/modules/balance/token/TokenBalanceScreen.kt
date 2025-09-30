package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.DeemedValue
import io.horizontalsystems.bankwallet.modules.balance.ZcashLockedValue
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceActionButton
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.send.zcash.shield.ShieldZcashFragment
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.transactions.transactionList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.TextAttention
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.ButtonsGroup
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsElementAmountText
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenBalanceScreen(
    viewModel: TokenBalanceViewModel,
    transactionsViewModel: TransactionsViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val view = LocalView.current
    val loading = uiState.balanceViewItem?.syncingProgress?.progress != null

    LaunchedEffect(uiState.failedIconVisible) {
        if (uiState.failedIconVisible) {
            val wallet = uiState.balanceViewItem?.wallet
            val errorMessage = uiState.failedErrorMessage

            wallet?.let {
                navController.slideFromBottom(
                    R.id.syncErrorDialog,
                    SyncErrorDialog.Input(wallet, errorMessage)
                )
            }
        }
    }

    HSScaffold(
        title = uiState.title,
        onBack = navController::popBackStack,
        menuItems = buildList {
            when {
                loading -> {
                    add(MenuItemLoading)
                }
                uiState.failedIconVisible -> {
                    add(
                        MenuItem(
                            icon = R.drawable.ic_warning_filled_24,
                            title = TranslatableString.ResString(R.string.BalanceSyncError_Title),
                            tint = ComposeAppTheme.colors.lucian,
                            onClick = {
                                uiState.failedErrorMessage?.let {
                                    HudHelper.showErrorMessage(view, it)
                                }
                            }
                        )
                    )
                }
            }

            if (uiState.balanceViewItem?.isWatchAccount == true) {
                add(
                    MenuItem(
                        icon = R.drawable.ic_balance_chart_24,
                        title = TranslatableString.ResString(R.string.Coin_Info),
                        onClick = {
                            val coinUid = uiState.balanceViewItem.wallet.coin.uid
                            val arguments = CoinFragment.Input(coinUid)

                            navController.slideFromRight(R.id.coinFragment, arguments)

                            stat(
                                page = StatPage.TokenPage,
                                event = StatEvent.OpenCoin(coinUid)
                            )
                        }
                    )
                )
            }
        }
    ) {
        val onClickReceive = {
            try {
                val wallet = viewModel.getWalletForReceive()
                navController.slideFromRight(R.id.receiveFragment, ReceiveFragment.Input(wallet))

                stat(page = StatPage.TokenPage, event = StatEvent.OpenReceive(wallet.token))
            } catch (e: BackupRequiredError) {
                val text = Translator.getString(
                    R.string.ManageAccount_BackupRequired_Description,
                    e.account.name,
                    e.coinTitle
                )
                navController.slideFromBottom(
                    R.id.backupRequiredDialog,
                    BackupRequiredDialog.Input(e.account, text)
                )

                stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.BackupRequired))
            }
        }

        val transactionItems = uiState.transactions
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            item {
                uiState.balanceViewItem?.let {
                    TokenBalanceHeader(
                        balanceViewItem = it,
                        navController = navController,
                        viewModel = viewModel,
                        showBottomSheet = { content ->
                            bottomSheetContent = content
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        },
                        hideBottomSheet = {
                            bottomSheetContent = null
                            coroutineScope.launch {
                                infoModalBottomSheetState.hide()
                            }
                        },
                        onClickReceive = onClickReceive,
                        loading = loading
                    )

                    if (it.isWatchAccount) {
                        uiState.receiveAddress?.let { receiveAddress ->
                            CellPrimary(
                                middle = {
                                    CellMiddleInfoTextIcon(text = stringResource(R.string.Balance_ReceiveAddress).hs)
                                },
                                right = {
                                    CellRightNavigation(
                                        subtitle = receiveAddress.shorten().hs(color = ComposeAppTheme.colors.leah)
                                    )
                                },
                                backgroundColor = ComposeAppTheme.colors.tyler,
                                onClick = onClickReceive
                            )
                        }
                    }

                    uiState.warningMessage?.let { warning ->
                        TextAttention(
                            modifier = Modifier
                                .background(ComposeAppTheme.colors.tyler)
                                .padding(16.dp),
                            text = warning
                        )
                    }
                }
            }

            if (transactionItems.isNullOrEmpty()) {
                item {
                    uiState.error?.let {
                        VSpacer(82.dp)
                        CardsErrorMessageDefault(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 64.dp),
                            icon = painterResource(R.drawable.warning_filled_24),
                            iconTint = ComposeAppTheme.colors.grey,
                            title = it.errorTitle,
                            text = it.message,
                        )
                    }
                }
            } else {
                transactionList(
                    transactionsMap = transactionItems,
                    willShow = { viewModel.willShow(it) },
                    onClick = {
                        onTransactionClick(
                            it,
                            viewModel,
                            transactionsViewModel,
                            navController
                        )
                    },
                    onBottomReached = { viewModel.onBottomReached() }
                )
            }
        }
    }
    bottomSheetContent?.let { info ->
        InfoBottomSheet(
            content = info,
            bottomSheetState = infoModalBottomSheetState,
            hideBottomSheet = {
                coroutineScope.launch {
                    infoModalBottomSheetState.hide()
                }
                bottomSheetContent = null
            }
        )
    }

}


private fun onTransactionClick(
    transactionViewItem: TransactionViewItem,
    tokenBalanceViewModel: TokenBalanceViewModel,
    transactionsViewModel: TransactionsViewModel,
    navController: NavController
) {
    val transactionItem = tokenBalanceViewModel.getTransactionItem(transactionViewItem) ?: return
    transactionsViewModel.tmpTransactionRecordToShow = transactionItem.record

    navController.slideFromBottom(R.id.transactionInfoFragment)

    stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.TransactionInfo))
}

@Composable
private fun TokenBalanceHeader(
    balanceViewItem: BalanceViewItem,
    navController: NavController,
    viewModel: TokenBalanceViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit = { _ -> },
    hideBottomSheet: () -> Unit,
    onClickReceive: () -> Unit,
    loading: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        val title: HSString
        val body: HSString

        if (balanceViewItem.balanceHidden || balanceViewItem.primaryValue == null) {
            title = "* * *".hs
            body = "".hs
        } else {
            val color = if (loading) {
                ComposeAppTheme.colors.andy
            } else if (balanceViewItem.primaryValue.dimmed) {
                ComposeAppTheme.colors.grey
            } else {
                null
            }

            title = balanceViewItem.primaryValue.value.hs(color = color)
            body = (balanceViewItem.syncingLineText ?: balanceViewItem.secondaryValue?.value ?: "").hs(color = color)
        }

        CardsElementAmountText(
            title = title,
            body = body,
            onClickTitle = {
                viewModel.toggleBalanceVisibility()
                HudHelper.vibrate(context)

                stat(page = StatPage.TokenPage, event = StatEvent.ToggleBalanceHidden)
            },
            onClickSubtitle = {
                viewModel.toggleBalanceVisibility()
                HudHelper.vibrate(context)

                stat(page = StatPage.TokenPage, event = StatEvent.ToggleBalanceHidden)
            },
        )

        if (!balanceViewItem.isWatchAccount) {
            ButtonsRow(
                viewItem = balanceViewItem,
                navController = navController,
                onClickReceive = onClickReceive
            )
        }

        LockedBalanceSection(
            balanceViewItem = balanceViewItem,
            navController = navController,
            showBottomSheet = showBottomSheet,
            hideBottomSheet = hideBottomSheet
        )
    }
}

@Composable
private fun LockedBalanceSection(
    balanceViewItem: BalanceViewItem,
    navController: NavController,
    showBottomSheet: (BottomSheetContent) -> Unit = { _ -> },
    hideBottomSheet: () -> Unit
) {
    if (balanceViewItem.lockedValues.isNotEmpty()) {
        balanceViewItem.lockedValues.forEach { lockedValue ->
            val infoTitle = lockedValue.infoTitle.getString()
            val infoText = lockedValue.info.getString()
            val actionButtonTitle: String?
            val onClickActionButton: (() -> Unit)?

            if (lockedValue is ZcashLockedValue) {
                actionButtonTitle =
                    stringResource(R.string.Balance_Zcash_UnshieldedBalance_Shield)
                onClickActionButton = {
                    hideBottomSheet.invoke()

                    navController.slideFromRight(
                        R.id.shieldZcash,
                        ShieldZcashFragment.Input(
                            balanceViewItem.wallet,
                            R.id.tokenBalanceFragment
                        )
                    )
                }
            } else {
                actionButtonTitle = null
                onClickActionButton = null
            }

            LockedBalanceCell(
                title = lockedValue.title.getString(),
                lockedAmount = lockedValue.coinValue,
                balanceHidden = balanceViewItem.balanceHidden
            ) {
                showBottomSheet.invoke(
                    BottomSheetContent(
                        icon = R.drawable.ic_info_24,
                        title = infoTitle,
                        description = infoText,
                        actionButtonTitle = actionButtonTitle,
                        onClickActionButton = onClickActionButton
                    )
                )
            }
        }
    }
}

data class BottomSheetContent(
    val icon: Int,
    val title: String,
    val description: String,
    val actionButtonTitle: String? = null,
    val onClickActionButton: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    content: BottomSheetContent,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(content.icon),
            title = content.title,
            titleColor = ComposeAppTheme.colors.leah,
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            onCloseClick = hideBottomSheet
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                body_bran(
                    text = content.description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
                VSpacer(56.dp)
                content.actionButtonTitle?.let {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = content.actionButtonTitle,
                        onClick = content.onClickActionButton ?: {}
                    )
                    VSpacer(32.dp)
                }

            }
        }
    }
}

@Composable
private fun LockedBalanceCell(
    title: String,
    lockedAmount: DeemedValue<String>,
    balanceHidden: Boolean,
    onClickInfo: () -> Unit
) {
    BoxBordered(bottom = true) {
        CellPrimary(
            middle = {
                CellMiddleInfoTextIcon(
                    text = title.hs,
                    icon = painterResource(R.drawable.info_filled_24),
                    iconTint = ComposeAppTheme.colors.grey,
                    onIconClick = onClickInfo,
                )
            },
            right = {
                CellRightInfoTextIcon(
                    text = if (!balanceHidden) lockedAmount.value.hs(dimmed = lockedAmount.dimmed) else "*****".hs,
                )
            },
            backgroundColor = ComposeAppTheme.colors.lawrence
        )
    }
}

@Composable
private fun ButtonsRow(
    viewItem: BalanceViewItem,
    navController: NavController,
    onClickReceive: () -> Unit
) {
    ButtonsGroup {
        BalanceActionButton(
            variant = ButtonVariant.Primary,
            icon = R.drawable.ic_balance_chart_24,
            title = stringResource(R.string.Coin_Chart),
            enabled = !viewItem.wallet.token.isCustom,
            onClick = {
                val coinUid = viewItem.wallet.coin.uid
                val arguments = CoinFragment.Input(coinUid)

                navController.slideFromRight(R.id.coinFragment, arguments)

                stat(page = StatPage.TokenPage, event = StatEvent.OpenCoin(coinUid))
            },
        )
        BalanceActionButton(
            variant = ButtonVariant.Secondary,
            icon = R.drawable.ic_arrow_down_24,
            title = stringResource(R.string.Balance_Receive),
            onClick = onClickReceive,
        )
        BalanceActionButton(
            variant = ButtonVariant.Secondary,
            icon = R.drawable.ic_arrow_up_24,
            title = stringResource(R.string.Balance_Send),
            onClick = {
                val sendTitle = Translator.getString(
                    R.string.Send_Title,
                    viewItem.wallet.token.fullCoin.coin.code
                )
                navController.slideFromRight(
                    R.id.enterAddressFragment,
                    EnterAddressFragment.Input(
                        wallet = viewItem.wallet,
                        title = sendTitle
                    )
                )

                stat(
                    page = StatPage.TokenPage,
                    event = StatEvent.OpenSend(viewItem.wallet.token)
                )
            },
        )
        if (viewItem.swapVisible) {
            BalanceActionButton(
                variant = ButtonVariant.Secondary,
                icon = R.drawable.ic_swap_circle_24,
                title = stringResource(R.string.Swap),
                onClick = {
                    navController.slideFromRight(R.id.multiswap, viewItem.wallet.token)

                    stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.Swap))
                },
            )
        }
    }
}
