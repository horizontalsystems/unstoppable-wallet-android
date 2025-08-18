package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tonapps.tonkeeper.api.shortAddress
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
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceActionOrangeButton
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
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.DoubleText
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TokenBalanceErrorView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.title3_leah
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
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
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = {
                    title3_leah(
                        text = uiState.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                stateIcon = {
                    if (uiState.balanceViewItem?.syncingProgress?.progress != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ComposeAppTheme.colors.grey,
                            strokeWidth = 2.dp
                        )
                    } else if (uiState.failedIconVisible) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning_filled_24),
                            contentDescription = "sync failed icon",
                            tint = ComposeAppTheme.colors.lucian
                        )
                    }
                },
                menuItems = buildList {
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
            )
        },
    ) { paddingValues ->
        val transactionItems = uiState.transactions
        if (transactionItems.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
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
                    TokenBalanceErrorView(
                        modifier = Modifier.background(ComposeAppTheme.colors.lawrence),
                        text = it.message,
                        title = it.errorTitle,
                        icon = R.drawable.ic_warning_filled_24,
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence),
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
        if (balanceViewItem.primaryValue.visible) {
            val body = if (balanceViewItem.syncingTextValue != null) {
                balanceViewItem.syncingTextValue + (balanceViewItem.syncedUntilTextValue?.let { " - $it" }
                    ?: "")
            } else {
                balanceViewItem.secondaryValue.value
            }
            DoubleText(
                title = balanceViewItem.primaryValue.value,
                body = body,
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
            DoubleText(
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

        ButtonsRow(
            viewItem = balanceViewItem,
            navController = navController,
            viewModel = viewModel,
            showBottomSheet = showBottomSheet
        )
        LockedBalanceSection(
            balanceViewItem = balanceViewItem,
            navController = navController,
            showBottomSheet = showBottomSheet,
            hideBottomSheet = hideBottomSheet
        )
    }
}

@Composable
fun WatchAddressCell(
    address: String,
    onInfoClick: () -> Unit,
    onCopyClick: (String) -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.lawrence)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead1_grey(
            text = stringResource(R.string.Balance_WalletAddress),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HSpacer(8.dp)
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = onInfoClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info_filled_20),
                contentDescription = "info button",
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(
            modifier = Modifier.weight(1f)
        )
        headline2_leah(
            text = address.shortAddress,
            modifier = Modifier.clickable {
                onCopyClick.invoke(address)
            }
        )
        HSpacer(8.dp)
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                onCopyClick.invoke(address)
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_copy_filled_20),
                contentDescription = "copy button",
                tint = ComposeAppTheme.colors.grey
            )
        }
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
                lockedAmount = lockedValue.coinValue
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
    onClickInfo: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.lawrence)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HSpacer(8.dp)
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = onClickInfo
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
            color = if (lockedAmount.dimmed) ComposeAppTheme.colors.andy else ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.subheadR,
            maxLines = 1,
        )
    }
    HsDivider()
}

@Composable
private fun ButtonsRow(
    viewItem: BalanceViewItem,
    navController: NavController,
    viewModel: TokenBalanceViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit
) {
    val view = LocalView.current
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

    if (viewItem.isWatchAccount) {
        val infoTitle = stringResource(R.string.Balance_WalletAddress)
        val infoDescription = ""
        viewModel.uiState.receiveAddress?.let { address ->
            WatchAddressCell(
                address = address,
                onInfoClick = {
                    showBottomSheet.invoke(
                        BottomSheetContent(
                            icon = R.drawable.ic_info_24,
                            title = infoTitle,
                            description = infoDescription,
                            actionButtonTitle = null,
                            onClickActionButton = null
                        )
                    )
                },
                onCopyClick = { copiedAddress ->
                    TextHelper.copyText(copiedAddress)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                    stat(
                        page = StatPage.TokenPage,
                        event = StatEvent.CopyAddress(viewItem.wallet.token.blockchain.uid)
                    )
                }
            )
        }
        HsDivider()
    } else {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BalanceActionOrangeButton(
                icon = R.drawable.ic_arrow_down_24,
                title = stringResource(R.string.Balance_Receive),
                onClick = onClickReceive,
            )
            BalanceActionButton(
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
                    icon = R.drawable.ic_swap_circle_24,
                    title = stringResource(R.string.Swap),
                    enabled = viewItem.swapEnabled,
                    onClick = {
                        navController.slideFromRight(R.id.multiswap, viewItem.wallet.token)

                        stat(page = StatPage.TokenPage, event = StatEvent.Open(StatPage.Swap))
                    },
                )
            }
            BalanceActionButton(
                icon = R.drawable.ic_balance_chart_24,
                title = stringResource(R.string.Coin_Info),
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
}
