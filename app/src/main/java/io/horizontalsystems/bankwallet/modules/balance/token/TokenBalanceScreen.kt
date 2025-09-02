package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.providers.Translator
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
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.transactions.transactionList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsElementAmountText
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
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

    HSScaffold(
        title = uiState.title,
        onBack = navController::popBackStack,
        menuItems = buildList {
            when {
                uiState.balanceViewItem?.syncingProgress?.progress != null -> {
                    add(MenuItemLoading)
                }
                uiState.failedIconVisible -> {
                    add(
                        MenuItem(
                            icon = R.drawable.ic_warning_filled_24,
                            title = TranslatableString.ResString(R.string.BalanceSyncError_Title),
                            tint = ComposeAppTheme.colors.lucian,
                            onClick = {
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
        val transactionItems = uiState.transactions
        if (transactionItems.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
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
                        }
                    )
                }
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
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
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
                            }
                        )
                    }
                }

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
    hideBottomSheet: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = 12.dp)
        if (!balanceViewItem.balanceHidden && balanceViewItem.primaryValue != null) {
            val body = if (balanceViewItem.syncingTextValue != null) {
                balanceViewItem.syncingTextValue + (balanceViewItem.syncedUntilTextValue?.let { " - $it" }
                    ?: "")
            } else {
                balanceViewItem.secondaryValue?.value
            }
            CardsElementAmountText(
                title = balanceViewItem.primaryValue.value,
                body = body ?: "",
                dimmed = balanceViewItem.primaryValue.dimmed,
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
        } else {
            CardsElementAmountText(
                title = "------",
                body = "",
                dimmed = false,
                onClickTitle = {
                    viewModel.toggleBalanceVisibility()
                    HudHelper.vibrate(context)

                    stat(page = StatPage.TokenPage, event = StatEvent.ToggleBalanceHidden)
                },
                onClickSubtitle = {

                }
            )
        }

        if (!balanceViewItem.isWatchAccount) {
            ButtonsRow(
                viewItem = balanceViewItem,
                navController = navController,
                viewModel = viewModel
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
    viewModel: TokenBalanceViewModel
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

    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
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
            enabled = viewItem.sendEnabled,
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
                enabled = viewItem.swapEnabled,
                onClick = {
                    navController.slideFromRight(R.id.multiswap, viewItem.wallet.token)

                    stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.Swap))
                },
            )
        }
    }
}
