package io.horizontalsystems.bankwallet.modules.balance.token

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewModel
import io.horizontalsystems.bankwallet.modules.balance.DeemedValue
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.manageaccount.dialogs.BackupRequiredDialog
import io.horizontalsystems.bankwallet.modules.send.address.EnterAddressFragment
import io.horizontalsystems.bankwallet.modules.syncerror.SyncErrorDialog
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.modules.transactions.transactionList
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.helpers.HudHelper


@Composable
fun TokenBalanceScreen(
    viewModel: TokenBalanceViewModel,
    transactionsViewModel: TransactionsViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = uiState.title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        }
    ) { paddingValues ->
        val transactionItems = uiState.transactions
        if (transactionItems.isNullOrEmpty()) {
            Column(Modifier.padding(paddingValues)) {
                uiState.balanceViewItem?.let {
                    TokenBalanceHeader(balanceViewItem = it, navController = navController, viewModel = viewModel)
                }
                if (transactionItems == null) {
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
        } else {
            val listState = rememberLazyListState()
            LazyColumn(Modifier.padding(paddingValues), state = listState) {
                item {
                    uiState.balanceViewItem?.let {
                        TokenBalanceHeader(balanceViewItem = it, navController = navController, viewModel = viewModel)
                    }
                }

                transactionList(
                    transactionsMap = transactionItems,
                    willShow = { viewModel.willShow(it) },
                    onClick = { onTransactionClick(it, viewModel, transactionsViewModel, navController) },
                    onBottomReached = { viewModel.onBottomReached() }
                )
            }
        }
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
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = (24.dp))
        WalletIcon(
            viewItem = balanceViewItem,
            viewModel = viewModel,
            navController = navController,
        )
        VSpacer(height = 12.dp)
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        viewModel.toggleBalanceVisibility()
                        HudHelper.vibrate(context)

                        stat(page = StatPage.TokenPage, event = StatEvent.ToggleBalanceHidden)
                    }
                ),
            text = if (balanceViewItem.primaryValue.visible) balanceViewItem.primaryValue.value else "*****",
            color = if (balanceViewItem.primaryValue.dimmed) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.title2R,
            textAlign = TextAlign.Center,
        )
        VSpacer(height = 6.dp)
        if (balanceViewItem.syncingTextValue != null) {
            body_grey(
                text = balanceViewItem.syncingTextValue + (balanceViewItem.syncedUntilTextValue?.let { " - $it" } ?: ""),
                maxLines = 1,
            )
        } else {
            Text(
                text = if (balanceViewItem.secondaryValue.visible) balanceViewItem.secondaryValue.value else "*****",
                color = if (balanceViewItem.secondaryValue.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.body,
                maxLines = 1,
            )
        }
        VSpacer(height = 24.dp)
        ButtonsRow(viewItem = balanceViewItem, navController = navController, viewModel = viewModel)
        LockedBalanceSection(balanceViewItem, navController)
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
private fun LockedBalanceSection(balanceViewItem: BalanceViewItem, navController: NavController) {
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
                    navController = navController
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
    navController: NavController
) {

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
            onClick = {
                navController.slideFromBottom(R.id.feeSettingsInfoDialog, FeeSettingsInfoDialog.Input(infoTitle, infoText))
            }
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
}

@Composable
private fun WalletIcon(
    viewItem: BalanceViewItem,
    viewModel: TokenBalanceViewModel,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .height(52.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        viewItem.syncingProgress.progress?.let { progress ->
            AndroidView(
                modifier = Modifier
                    .size(52.dp),
                factory = { context ->
                    RotatingCircleProgressView(context)
                },
                update = { view ->
                    val color = when (viewItem.syncingProgress.dimmed) {
                        true -> R.color.grey_50
                        false -> R.color.grey
                    }
                    view.setProgressColored(progress, view.context.getColor(color))
                }
            )
        }
        if (viewItem.failedIconVisible) {
            val view = LocalView.current
            Image(
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onSyncErrorClicked(viewItem, viewModel, navController, view)
                    },
                painter = painterResource(id = R.drawable.ic_attention_24),
                contentDescription = "coin icon",
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.lucian)
            )
        } else {
            CoinImage(
                token = viewItem.wallet.token,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun onSyncErrorClicked(viewItem: BalanceViewItem, viewModel: TokenBalanceViewModel, navController: NavController, view: View) {
    when (val syncErrorDetails = viewModel.getSyncErrorDetails(viewItem)) {
        is BalanceViewModel.SyncError.Dialog -> {
            val wallet = syncErrorDetails.wallet
            val errorMessage = syncErrorDetails.errorMessage

            navController.slideFromBottom(
                R.id.syncErrorDialog,
                SyncErrorDialog.Input(wallet, errorMessage)
            )
        }

        is BalanceViewModel.SyncError.NetworkNotAvailable -> {
            HudHelper.showErrorMessage(view, R.string.Hud_Text_NoInternet)
        }
    }
}


@Composable
private fun ButtonsRow(viewItem: BalanceViewItem, navController: NavController, viewModel: TokenBalanceViewModel) {
    val onClickReceive = {
        try {
            val wallet = viewModel.getWalletForReceive()
            navController.slideFromRight(R.id.receiveFragment, wallet)

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
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp)
    ) {
        if (viewItem.isWatchAccount) {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Address),
                onClick = onClickReceive,
            )
        } else {
            ButtonPrimaryYellow(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Send),
                onClick = {
                    val sendTitle = Translator.getString(R.string.Send_Title, viewItem.wallet.token.fullCoin.coin.code)
                    navController.slideFromRight(
                        R.id.enterAddressFragment,
                        EnterAddressFragment.Input(
                            wallet = viewItem.wallet,
                            title = sendTitle
                        )
                    )

                    stat(page = StatPage.TokenPage, event = StatEvent.OpenSend(viewItem.wallet.token))
                },
                enabled = viewItem.sendEnabled
            )
            HSpacer(8.dp)
            if (!viewItem.swapVisible) {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.Balance_Receive),
                    onClick =  onClickReceive,
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
                        navController.slideFromRight(R.id.multiswap, viewItem.wallet.token)

                        stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.Swap))
                    },
                    enabled = viewItem.swapEnabled
                )
            }
        }
        HSpacer(8.dp)
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = !viewItem.wallet.token.isCustom,
            onClick = {
                val coinUid = viewItem.wallet.coin.uid
                val arguments = CoinFragment.Input(coinUid)

                navController.slideFromRight(R.id.coinFragment, arguments)

                stat(page = StatPage.TokenPage, event = StatEvent.OpenCoin(coinUid))
            },
        )
    }
}
