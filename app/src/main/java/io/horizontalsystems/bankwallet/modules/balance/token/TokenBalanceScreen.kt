package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BackupRequiredError
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.DeemedValue
import io.horizontalsystems.bankwallet.modules.balance.LockedValue
import io.horizontalsystems.bankwallet.modules.balance.StellarLockedValue
import io.horizontalsystems.bankwallet.modules.balance.ZcashLockedValue
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceActionButton
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.receive.ReceiveFragment
import io.horizontalsystems.bankwallet.modules.receive.ZcashAddressTypeSelectFragment
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.send.zcash.shield.ShieldZcashFragment
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.transactions.transactionList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.TextAttention
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BalanceButtonsGroup
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottombars.ButtonsGroupHorizontal
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsElementAmountText
import io.horizontalsystems.bankwallet.uiv3.components.cards.CardsErrorMessageDefault
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenBalanceScreen(
    viewModel: TokenBalanceViewModel,
    transactionsViewModel: TransactionsViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    var bottomSheetContent by remember { mutableStateOf<LockedValue?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val loading = uiState.balanceViewItem?.syncingProgress?.progress != null

    LaunchedEffect(uiState.failedIconVisible) {
        if (uiState.failedIconVisible) {
            openSyncErrorDialog(uiState, navController)
        }
    }

    LaunchedEffect(uiState.alertUnshieldedBalance) {
        if (uiState.alertUnshieldedBalance != null) {
            bottomSheetContent = ZcashLockedValue(
                title = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalanceDetected_Info_Title),
                info = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalance_Info_Description),
                coinValue = DeemedValue("", false)
            )
            coroutineScope.launch {
                delay(300)
                bottomSheetState.show()
            }
            viewModel.transparentZecAmountWarningShown(uiState.alertUnshieldedBalance)
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
                                openSyncErrorDialog(uiState, navController)
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
                if (wallet.token.blockchainType == BlockchainType.Zcash) {
                    navController.slideFromRight(
                        R.id.receiveSelectZcashAddressTypeFragment,
                        ZcashAddressTypeSelectFragment.Input(wallet)
                    )
                } else {
                    navController.slideFromRight(
                        R.id.receiveFragment,
                        ReceiveFragment.Input(wallet)
                    )
                }

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
                                bottomSheetState.show()
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
                                        subtitle = receiveAddress.shorten()
                                            .hs(color = ComposeAppTheme.colors.leah)
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
    bottomSheetContent?.let { lockedValue ->
        when (lockedValue) {
            is StellarLockedValue -> {
                StellarLockedBalanceBottomSheet(
                    sheetState = bottomSheetState,
                    stellarLockedValue = lockedValue,
                    onClose = {
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                        bottomSheetContent = null
                    }
                )
            }

            is ZcashLockedValue -> {
                ZcashLockedBalanceBottomSheet(
                    sheetState = bottomSheetState,
                    wallet = viewModel.wallet,
                    title = lockedValue.title.getString(),
                    info = lockedValue.info.getString(),
                    onShieldClick = { wallet ->
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                        bottomSheetContent = null
                        navController.slideFromRight(
                            R.id.shieldZcash,
                            ShieldZcashFragment.Input(wallet, R.id.tokenBalanceFragment)
                        )
                    },
                    onClose = {
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                        bottomSheetContent = null
                    }
                )
            }

            else -> {
                InfoBottomSheet(
                    lockedValue = lockedValue,
                    bottomSheetState = bottomSheetState,
                    hideBottomSheet = {
                        coroutineScope.launch {
                            bottomSheetState.hide()
                        }
                        bottomSheetContent = null
                    }
                )
            }
        }

    }

}

private fun openSyncErrorDialog(
    uiState: TokenBalanceModule.TokenBalanceUiState,
    navController: NavController
) {
    val wallet = uiState.balanceViewItem?.wallet
    val errorMessage = uiState.failedErrorMessage

    wallet?.let {
        navController.slideFromBottom(
            R.id.syncErrorDialog,
            SyncErrorDialog.Input(wallet, errorMessage)
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
    showBottomSheet: (LockedValue) -> Unit = { _ -> },
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
            body =
                (balanceViewItem.syncingTextValue ?: balanceViewItem.secondaryValue?.value ?: "").hs(
                    color = color
                )
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
            showBottomSheet = showBottomSheet,
        )
    }
}

@Composable
private fun LockedBalanceSection(
    balanceViewItem: BalanceViewItem,
    showBottomSheet: (LockedValue) -> Unit,
) {
    balanceViewItem.lockedValues.forEach { lockedValue ->

        when (lockedValue) {
            is ZcashLockedValue -> {
                LockedBalanceZcashCell(
                    title = lockedValue.title.getString(),
                    lockedAmount = lockedValue.coinValue,
                    balanceHidden = balanceViewItem.balanceHidden,
                ) {
                    showBottomSheet(lockedValue)
                }
            }

            else -> {
                LockedBalanceCell(
                    title = lockedValue.title.getString(),
                    lockedAmount = lockedValue.coinValue,
                    balanceHidden = balanceViewItem.balanceHidden,
                ) {
                    showBottomSheet(lockedValue)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    lockedValue: LockedValue,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {
    BottomSheetContent(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Understand),
                variant = ButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                onClick = hideBottomSheet
            )
        }
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.book_24),
            title = lockedValue.title.getString()
        )
        TextBlock(
            text = lockedValue.info.getString(),
            textAlign = TextAlign.Center
        )
        VSpacer(16.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZcashLockedBalanceBottomSheet(
    sheetState: SheetState,
    wallet: Wallet,
    title: String ,
    info: String,
    onShieldClick: (Wallet) -> Unit,
    onClose: () -> Unit
) {
    BottomSheetContent(
        onDismissRequest = onClose,
        sheetState = sheetState
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.ic_shield_off_24),
            title = title
        )
        TextBlock(
            text = info,
            textAlign = TextAlign.Center
        )
        VSpacer(16.dp)
        ButtonsGroupHorizontal {
            HSButton(
                title = stringResource(R.string.Button_Cancel),
                variant = ButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
                onClick = onClose
            )
            HSButton(
                title = stringResource(R.string.Balance_Zcash_UnshieldedBalance_Shield),
                variant = ButtonVariant.Primary,
                modifier = Modifier.weight(1f),
                onClick = {
                    onShieldClick(wallet)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StellarLockedBalanceBottomSheet(
    sheetState: SheetState,
    stellarLockedValue: StellarLockedValue,
    onClose: () -> Unit
) {
    BottomSheetContent(
        onDismissRequest = onClose,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Close),
                variant = ButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose
            )
        }
    ) {
        BottomSheetHeaderV3(
            title = stringResource(R.string.Info_Reserved_Title)
        )
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp)
        ) {
            stellarLockedValue.lockedValues.forEach { item ->
                CellSecondary(
                    middle = {
                        CellMiddleInfo(
                            eyebrow = item.title.hs
                        )
                    },
                    right = {
                        CellRightInfo(
                            title = item.value.hs
                        )
                    }
                )
            }
        }
        TextBlock(
            text = stringResource(R.string.Info_Reserved_Description),
        )
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
private fun LockedBalanceZcashCell(
    title: String,
    lockedAmount: DeemedValue<String>,
    balanceHidden: Boolean,
    onClickInfo: () -> Unit
) {
    BoxBordered(bottom = true) {
        CellPrimary(
            middle = {
                CellMiddleInfo(
                    eyebrow = title.hs,
                )
            },
            right = {
                CellRightInfoTextIcon(
                    text = if (!balanceHidden) lockedAmount.value.hs(
                        color = ComposeAppTheme.colors.jacob,
                        dimmed = lockedAmount.dimmed
                    ) else "*****".hs,
                    icon = painterResource(R.drawable.warning_filled_24),
                    iconTint = ComposeAppTheme.colors.jacob,
                    onIconClick = onClickInfo
                )
            },
            backgroundColor = ComposeAppTheme.colors.lawrence,
        )
    }
}

@Composable
private fun ButtonsRow(
    viewItem: BalanceViewItem,
    navController: NavController,
    onClickReceive: () -> Unit
) {
    BalanceButtonsGroup {
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun InfoBottomSheetPreview() {
    val bottomSheetState = rememberModalBottomSheetState()
    val lockedValue = LockedValue(
        title = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalanceDetected_Info_Title),
        info = TranslatableString.ResString(R.string.Balance_Zcash_UnshieldedBalance_Info_Description),
        coinValue = DeemedValue("", false)
    )

    ComposeAppTheme {
        InfoBottomSheet(
            lockedValue,
            hideBottomSheet = {},
            bottomSheetState,
        )
    }
}