package cash.p.terminal.modules.balance.token

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.isCustom
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.modules.balance.BackupRequiredError
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewModel
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.manageaccount.dialogs.BackupRequiredDialog
import cash.p.terminal.modules.receive.address.ReceiveAddressFragment
import cash.p.terminal.modules.send.SendFragment
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.syncerror.SyncErrorDialog
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.modules.transactions.transactionList
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryCircle
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithIcon
import cash.p.terminal.ui.compose.components.CoinImage
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.body_grey
import cash.p.terminal.ui.compose.components.subhead2_grey
import cash.p.terminal.ui.extensions.RotatingCircleProgressView
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
                title = TranslatableString.PlainString(uiState.title),
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
    transactionsViewModel  : TransactionsViewModel,
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
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WalletIcon(
            viewItem = balanceViewItem,
            viewModel = viewModel,
            navController = navController,
        )
        VSpacer(height = 6.dp)
        Text(
            modifier = Modifier
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
            maxLines = 1,
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
        LockedBalanceCell(balanceViewItem)
    }
}

@Composable
private fun LockedBalanceCell(balanceViewItem: BalanceViewItem) {
    if (balanceViewItem.coinValueLocked.value != null) {
        VSpacer(height = 8.dp)
        RowUniversal(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
        ) {
            subhead2_grey(
                text = stringResource(R.string.Balance_LockedAmount_Title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = if (balanceViewItem.coinValueLocked.visible) balanceViewItem.coinValueLocked.value else "*****",
                color = if (balanceViewItem.coinValueLocked.dimmed) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
        VSpacer(height = 16.dp)
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
                iconUrl = viewItem.coinIconUrl,
                placeholder = viewItem.coinIconPlaceholder,
                modifier = Modifier
                    .size(32.dp)
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
                SyncErrorDialog.prepareParams(wallet, errorMessage)
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
            val params = ReceiveAddressFragment.params(viewModel.getWalletForReceive(viewItem))
            navController.slideFromRight(R.id.receiveFragment, params)
        } catch (e: BackupRequiredError) {
            val text = Translator.getString(
                R.string.ManageAccount_BackupRequired_Description,
                e.account.name,
                e.coinTitle
            )
            navController.slideFromBottom(
                R.id.backupRequiredDialog,
                BackupRequiredDialog.prepareParams(e.account, text)
            )
        }
    }

    Row(
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (viewItem.isWatchAccount) {
            ButtonPrimaryDefault(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.Balance_Address),
                onClick = onClickReceive,
            )
        } else {
            ButtonPrimaryYellowWithIcon(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_arrow_up_right_24,
                title = stringResource(R.string.Balance_Send),
                onClick = {
                    navController.slideFromRight(
                        R.id.sendXFragment,
                        SendFragment.prepareParams(viewItem.wallet)
                    )
                },
                enabled = viewItem.sendEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            ButtonPrimaryCircle(
                icon = R.drawable.ic_arrow_down_left_24,
                contentDescription = stringResource(R.string.Balance_Receive),
                onClick = onClickReceive,
            )
            if (viewItem.swapVisible) {
                Spacer(modifier = Modifier.width(8.dp))
                ButtonPrimaryCircle(
                    icon = R.drawable.ic_swap_24,
                    contentDescription = stringResource(R.string.Swap),
                    onClick = {
                        navController.slideFromRight(
                            R.id.swapFragment,
                            SwapMainModule.prepareParams(viewItem.wallet.token)
                        )
                    },
                    enabled = viewItem.swapEnabled
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        ButtonPrimaryCircle(
            icon = R.drawable.ic_chart_24,
            contentDescription = stringResource(R.string.Coin_Info),
            enabled = !viewItem.wallet.token.isCustom,
            onClick = {
                val coinUid = viewItem.wallet.coin.uid
                val arguments = CoinFragment.prepareParams(coinUid)

                navController.slideFromRight(R.id.coinFragment, arguments)
            },
        )
    }
}
