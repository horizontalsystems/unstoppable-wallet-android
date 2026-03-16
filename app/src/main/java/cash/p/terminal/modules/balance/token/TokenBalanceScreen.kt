package cash.p.terminal.modules.balance.token

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.MainGraphDirections
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.premiumAction
import cash.p.terminal.modules.balance.BackupRequiredError
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.displayoptions.DisplayDiffOptionType
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.manageaccount.dialogs.BackupRequiredDialog
import cash.p.terminal.modules.receive.ReceiveFragment
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.syncerror.SyncErrorDialog
import cash.p.terminal.modules.transactions.AmlCheckInfoBottomSheet
import cash.p.terminal.modules.transactions.AmlCheckPromoBanner
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.modules.transactions.transactionList
import cash.p.terminal.modules.transactions.transactionsHiddenBlock
import cash.p.terminal.navigation.entity.SwapParams
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.ui.compose.components.CoinIconWithSyncProgress
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.components.diffColor
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HSSwipeRefresh
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoBottomSheet
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SnackbarDuration
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_grey
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.balance.DeemedValue
import cash.p.terminal.wallet.isCosanta
import cash.p.terminal.wallet.isPirateCash

@Composable
fun TokenBalanceScreen(
    viewModel: TokenBalanceViewModel,
    transactionsViewModel: TransactionsViewModel,
    sendResult: SendResult? = viewModel.sendResult,
    navController: NavController,
    refreshing: Boolean,
    onStackingClicked: () -> Unit,
    onShowAllTransactionsClicked: () -> Unit,
    onClickSubtitle: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val view = LocalView.current

    var showAmlInfoSheet by remember { mutableStateOf(false) }

    val failedIconVisible = uiState.balanceViewItem?.failedIconVisible == true
    val loading = uiState.balanceViewItem?.syncingProgress?.progress != null

    LaunchedEffect(failedIconVisible) {
        if (failedIconVisible) {
            onSyncErrorClicked(uiState.balanceViewItem, viewModel, navController)
        }
    }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = uiState.title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = buildList {
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(
                                if (uiState.isFavorite) R.string.CoinPage_Unfavorite else R.string.CoinPage_Favorite
                            ),
                            icon = if (uiState.isFavorite) R.drawable.ic_star_filled_20 else R.drawable.ic_star_20,
                            tint = if (uiState.isFavorite) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.grey,
                            onClick = { viewModel.toggleFavorite() }
                        )
                    )
                    if (!uiState.isCustomToken) {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.Coin_Info),
                                icon = R.drawable.ic_chart_24,
                                onClick = {
                                    val coinUid = uiState.balanceViewItem?.wallet?.coin?.uid ?: return@MenuItem
                                    val arguments = CoinFragmentInput(coinUid)
                                    navController.slideFromRight(R.id.coinFragment, arguments)
                                }
                            )
                        )
                    }
                    add(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Settings_Title),
                            icon = R.drawable.ic_manage_2_24,
                            onClick = onSettingsClick
                        )
                    )
                    if (failedIconVisible && !loading) {
                        add(
                            MenuItem(
                                title = TranslatableString.ResString(R.string.BalanceSyncError_Title),
                                icon = R.drawable.ic_attention_red_24,
                                tint = ComposeAppTheme.colors.lucian,
                                onClick = {
                                    onSyncErrorClicked(uiState.balanceViewItem, viewModel, navController)
                                }
                            )
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val transactionItems = uiState.transactions
        when (sendResult) {
            SendResult.Sending -> {
                HudHelper.showInProcessMessage(
                    view,
                    R.string.Send_Sending,
                    SnackbarDuration.INDEFINITE
                )
            }

            is SendResult.Sent -> {
                HudHelper.showSuccessMessage(
                    view,
                    R.string.Send_Success,
                    SnackbarDuration.MEDIUM
                )
            }

            is SendResult.SentButQueued -> {
                HudHelper.showWarningMessage(
                    view,
                    R.string.send_success_queued,
                    SnackbarDuration.LONG
                )
            }

            is SendResult.Failed -> {
                HudHelper.showErrorMessage(
                    view,
                    sendResult.caution.getDescription() ?: sendResult.caution.getString()
                )
            }

            null -> Unit
        }
        if (transactionItems == null || (transactionItems.isEmpty() && !uiState.hasHiddenTransactions)) {
            HSSwipeRefresh(
                refreshing = refreshing,
                modifier = Modifier.padding(paddingValues),
                onRefresh = onRefresh
            ) {
                Column {
                    uiState.balanceViewItem?.let {
                        TokenBalanceHeader(
                            balanceViewItem = it,
                            navController = navController,
                            viewModel = viewModel,
                            uiState = uiState,
                            onStackingClicked = onStackingClicked,
                            onClickSubtitle = onClickSubtitle,
                            isShowShieldFunds = uiState.isShowShieldFunds
                        )
                    }
                    if (transactionItems == null || uiState.syncing) {
                        ListEmptyView(
                            text = stringResource(R.string.Transactions_WaitForSync),
                            icon = R.drawable.ic_clock
                        )
                    } else {
                        ListEmptyView(
                            text = stringResource(R.string.Transactions_EmptyList),
                            icon = R.drawable.ic_outgoingraw
                        )
                    }
                }
            }
        } else {
            HSSwipeRefresh(
                refreshing = refreshing,
                modifier = Modifier.padding(paddingValues),
                onRefresh = onRefresh
            ) {
                LazyColumn(state = rememberLazyListState()) {
                    item {
                        uiState.balanceViewItem?.let {
                            TokenBalanceHeader(
                                balanceViewItem = it,
                                navController = navController,
                                viewModel = viewModel,
                                uiState = uiState,
                                onStackingClicked = onStackingClicked,
                                onClickSubtitle = onClickSubtitle,
                                isShowShieldFunds = uiState.isShowShieldFunds
                            )
                        }
                    }

                    if (uiState.showAmlPromo) {
                        item {
                            AmlCheckPromoBanner(
                                amlCheckEnabled = uiState.amlCheckEnabled,
                                onToggleChange = { enabled ->
                                    if (enabled) {
                                        navController.premiumAction {
                                            viewModel.setAmlCheckEnabled(true)
                                        }
                                    } else {
                                        viewModel.setAmlCheckEnabled(false)
                                    }
                                },
                                onInfoClick = { showAmlInfoSheet = true },
                                onClose = {
                                    viewModel.dismissAmlPromo()
                                    HudHelper.showPremiumMessage(
                                        view,
                                        R.string.aml_promo_dismiss_hud,
                                        SnackbarDuration.LONG
                                    )
                                },
                                modifier = Modifier.padding(vertical = 12.dp)
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
                        isItemBalanceHidden = { !it.showAmount },
                        onSensitiveValueClick = {
                            HudHelper.vibrate(App.instance)
                            transactionsViewModel.toggleTransactionInfoHidden(it.uid)
                        },
                        onBottomReached = viewModel::onBottomReached
                    )
                    if (uiState.hasHiddenTransactions) {
                        transactionsHiddenBlock(
                            shortBlock = transactionItems.isNotEmpty(),
                            onShowAllTransactionsClicked = onShowAllTransactionsClicked
                        )
                    }
                }
            }
        }
    }

    if (showAmlInfoSheet) {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {
                showAmlInfoSheet = false
                navController.slideFromRight(
                    R.id.premiumSettingsFragment
                )
            },
            onLaterClick = { showAmlInfoSheet = false },
            onDismiss = { showAmlInfoSheet = false }
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
    transactionsViewModel.tmpItemToShow = transactionItem

    navController.slideFromBottom(R.id.transactionInfoFragment)
}

@Composable
private fun TokenBalanceHeader(
    balanceViewItem: BalanceViewItem,
    navController: NavController,
    viewModel: TokenBalanceViewModel,
    uiState: TokenBalanceModule.TokenBalanceUiState,
    onStackingClicked: () -> Unit,
    onClickSubtitle: () -> Unit,
    isShowShieldFunds: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // Sub-header row: coin icon + ticker + badge + staking status
        VSpacer(height = 12.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CoinIconWithSyncProgress(
                    token = balanceViewItem.wallet.token,
                    syncingProgress = balanceViewItem.syncingProgress,
                    failedIconVisible = balanceViewItem.failedIconVisible,
                    onClickSyncError = {
                        onSyncErrorClicked(balanceViewItem, viewModel, navController)
                    }
                )
            }
            HSpacer(16.dp)
            Text(
                text = uiState.coinCode + (uiState.badge?.let { " ($it)" } ?: ""),
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            uiState.stakingStatus?.let { status ->
                HSpacer(8.dp)
                val (text, color) = when (status) {
                    TokenBalanceModule.StakingStatus.ACTIVE -> Pair(
                        stringResource(R.string.staking_active),
                        ComposeAppTheme.colors.remus
                    )
                    TokenBalanceModule.StakingStatus.INACTIVE -> Pair(
                        stringResource(R.string.staking_inactive),
                        ComposeAppTheme.colors.lucian
                    )
                }
                Text(
                    text = text,
                    color = color,
                    style = ComposeAppTheme.typography.microSB,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Balance
        VSpacer(height = 22.dp)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)
                    }
                ),
            text = if (balanceViewItem.primaryValue.visible) balanceViewItem.primaryValue.value else "*****",
            color = if (balanceViewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title2R,
            textAlign = TextAlign.Start,
        )

        // Price line
        VSpacer(height = 6.dp)
        if (balanceViewItem.syncingTextValue != null) {
            body_grey(
                text = balanceViewItem.syncingTextValue + (balanceViewItem.syncedUntilTextValue?.let { " - $it" } ?: ""),
                maxLines = 1,
            )
        } else {
            Text(
                text = if (balanceViewItem.secondaryValue.visible) viewModel.secondaryValue.value else "*****",
                color = if (balanceViewItem.secondaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (balanceViewItem.secondaryValue.visible) {
                            onClickSubtitle()
                        }
                    }
                )
            )
        }

        // Exchange rate + diff
        if (balanceViewItem.exchangeValue.visible) {
            VSpacer(height = 4.dp)
            Row {
                Text(
                    text = "1${uiState.coinCode} = ${balanceViewItem.exchangeValue.value}",
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                )
                if (balanceViewItem.displayDiffOptionType != DisplayDiffOptionType.NONE) {
                    balanceViewItem.diff?.let { diff ->
                        HSpacer(width = 8.dp)
                        Text(
                            text = balanceViewItem.fullDiff,
                            color = diffColor(diff),
                            style = ComposeAppTheme.typography.subhead2,
                        )
                    }
                }
            }
        }

        // Staking unpaid row
        uiState.stakingUnpaid?.let { unpaid ->
            VSpacer(height = 21.dp)
            HorizontalDivider(color = ComposeAppTheme.colors.steel20, thickness = 1.dp)
            RowUniversal {
                subhead2_grey(
                    text = stringResource(R.string.staking_unpaid),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (balanceViewItem.primaryValue.visible) unpaid else "*****",
                    color = if (balanceViewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }

        VSpacer(height = 12.dp)
        ButtonsRow(
            viewItem = balanceViewItem,
            navController = navController,
            viewModel = viewModel,
            onStackingClicked = onStackingClicked,
            isShowShieldFunds = isShowShieldFunds
        )
        LockedBalanceSection(balanceViewItem)
        balanceViewItem.warning?.let {
            VSpacer(height = 8.dp)
            TextImportantWarning(
                icon = R.drawable.ic_attention_20,
                title = it.title.getString(),
                text = it.text.getString()
            )
        }
        VSpacer(height = 16.dp)
    }
}

@Composable
private fun LockedBalanceSection(balanceViewItem: BalanceViewItem) {
    if (balanceViewItem.lockedValues.isNotEmpty()) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
        ) {
            balanceViewItem.lockedValues.forEach { lockedValue ->
                LockedBalanceCell(
                    title = lockedValue.title.getString(),
                    infoTitle = lockedValue.infoTitle.getString(),
                    infoText = lockedValue.info.getString(),
                    lockedAmount = lockedValue.coinValue,
                )
            }
        }
    }
}

@Composable
private fun LockedBalanceCell(
    title: String,
    infoTitle: String,
    infoText: String,
    lockedAmount: DeemedValue<String>,
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp),
    ) {
        subhead2_grey(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HSpacer(8.dp)
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = { showInfoDialog = true }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = if (lockedAmount.visible) lockedAmount.value else "*****",
            color = if (lockedAmount.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.subhead2,
            maxLines = 1,
        )
    }

    if (showInfoDialog) {
        InfoBottomSheet(
            title = infoTitle,
            text = infoText,
            onDismiss = { showInfoDialog = false }
        )
    }
}

private fun onSyncErrorClicked(
    viewItem: BalanceViewItem,
    viewModel: TokenBalanceViewModel,
    navController: NavController
) {
    when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
        is BalanceViewModel.SyncError.Dialog -> {
            val wallet = syncErrorDetails.wallet
            val errorMessage = syncErrorDetails.errorMessage

            navController.slideFromBottom(
                R.id.syncErrorDialog,
                SyncErrorDialog.Input(wallet, errorMessage)
            )
        }
        is BalanceViewModel.SyncError.NetworkNotAvailable -> Unit // We already show this at bottom panel
    }
}


@Composable
private fun ButtonsRow(
    viewItem: BalanceViewItem,
    navController: NavController,
    viewModel: TokenBalanceViewModel,
    onStackingClicked: () -> Unit,
    isShowShieldFunds: Boolean
) {
    val onClickReceive = {
        try {
            val wallet = viewModel.getWalletForReceive()
            navController.slideFromRight(R.id.receiveFragment, ReceiveFragment.Input(wallet))
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
        }
    }

    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
    ) {
        if (viewItem.isWatchAccount) {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Address),
                onClick = onClickReceive,
            )
            if (viewItem.wallet.isCosanta() || viewItem.wallet.isPirateCash()) {
                HSpacer(8.dp)
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_coins_stacking,
                    contentDescription = stringResource(R.string.stacking),
                    onClick = {
                        onStackingClicked()
                    }
                )
            }
        } else {
            if (!viewItem.isSendDisabled) {
                ButtonPrimaryYellow(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Balance_Send),
                    onClick = {
                        val sendTitle = Translator.getString(
                            R.string.Send_Title,
                            viewItem.wallet.token.fullCoin.coin.code
                        )
                        navController.navigate(
                            MainGraphDirections.actionGlobalToSendFragment(
                                SendFragment.Input(
                                    wallet = viewItem.wallet,
                                    title = sendTitle,
                                    sendEntryPointDestId = R.id.tokenBalanceFragment,
                                    address = null
                                )
                            )
                        )
                    },
                    enabled = viewItem.sendEnabled
                )
                HSpacer(8.dp)
            }
            if (!viewItem.swapVisible) {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Balance_Receive),
                    onClick = onClickReceive,
                )
            } else {
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_arrow_down_left_24,
                    contentDescription = stringResource(R.string.Balance_Receive),
                    onClick = onClickReceive,
                )
            }
            if (viewItem.swapVisible) {
                HSpacer(8.dp)
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_swap_24,
                    contentDescription = stringResource(R.string.Swap),
                    onClick = {
                        navController.slideFromRight(
                            R.id.multiswap,
                            SwapParams.TOKEN_IN to viewItem.wallet.token
                        )
                    },
                    enabled = viewItem.swapEnabled
                )
            }
            if (viewItem.wallet.isCosanta() || viewItem.wallet.isPirateCash()) {
                HSpacer(8.dp)
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_coins_stacking,
                    contentDescription = stringResource(R.string.stacking),
                    onClick = {
                        onStackingClicked()
                    }
                )
            }
        }
    }
    if (isShowShieldFunds) {
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.shield_funds),
                onClick = viewModel::proposeShielding
            )
            body_grey(
                text = stringResource(R.string.typical_fee),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
