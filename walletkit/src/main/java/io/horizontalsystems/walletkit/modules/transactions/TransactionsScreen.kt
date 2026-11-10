package io.horizontalsystems.walletkit.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statTab
import io.horizontalsystems.walletkit.entities.ViewState
import io.horizontalsystems.walletkit.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.walletkit.modules.balance.BalanceModule
import io.horizontalsystems.walletkit.modules.balance.BalanceScreenState
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapInfoPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoPage
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoPayload
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HeaderStick
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.HsImage
import io.horizontalsystems.walletkit.ui.compose.components.ListEmptyView
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.TransactionIcon
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import io.horizontalsystems.walletkit.uiv3.components.pulsingTextColor
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabFolderItem
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsFolder
import kotlinx.coroutines.delay

private const val SlideDurationMillis = 300L

@Composable
fun TransactionsScreen(
    navigation: HSNavigation,
    viewModel: TransactionsViewModel,
) {
    val accountsViewModel =
        viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    // The view model lives as long as the main screen, so the tab is kept between visits.
    var selectedTab by rememberSaveable { mutableStateOf(viewModel.uiState.tab) }
    LaunchedEffect(selectedTab) {
        viewModel.setTab(selectedTab)
    }

    val showFilterAlertDot by viewModel.filterResetEnabled.observeAsState(false)

    val uiState = viewModel.uiState
    val syncing = uiState.syncing
    val transactions = uiState.transactions

    var listVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SlideDurationMillis)
        listVisible = true
    }

    HSScaffold(
        title = stringResource(R.string.Activity_Title),
        onBack = navigation::removeLastOrNull,
        menuItems = buildList {
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Transactions_Filter),
                    icon = R.drawable.ic_manage_2_24,
                    showAlertDot = showFilterAlertDot,
                    testTag = TestTags.TX_FILTER,
                    onClick = {
                        navigation.slideFromRight(TransactionsFilterPage)

                        stat(
                            page = StatPage.Transactions,
                            event = StatEvent.Open(StatPage.TransactionFilter)
                        )
                    },
                )
            )
        }
    ) {
        Column {
            ActivityTabs(
                selectedTab = selectedTab,
                onSelect = {
                    selectedTab = it

                    stat(
                        page = StatPage.Transactions,
                        event = StatEvent.SwitchTab(it.statTab)
                    )
                }
            )

            Crossfade(
                // The list waits for the view model to follow a tab switch. The outgoing content
                // reads the current state too, so it draws nothing while the tabs differ: the old
                // tab's rows never show, not even fading out, under the new tab.
                uiState.viewState.takeIf { uiState.tab == selectedTab },
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence),
                label = ""
            ) { viewState ->
                if (listVisible && viewState == ViewState.Success && uiState.tab == selectedTab) {
                    val pagedList = uiState.tab == ActivityTab.Transfers
                    transactions?.let { transactionItems ->
                        if (transactionItems.isEmpty()) {
                            if (syncing) {
                                ListEmptyView(
                                    modifier = Modifier
                                        .background(ComposeAppTheme.colors.lawrence)
                                        .testTag(TestTags.TX_WAITING_SYNC),
                                    text = stringResource(R.string.Transactions_WaitForSync),
                                    icon = R.drawable.ic_clock
                                )
                            } else {
                                ListEmptyView(
                                    modifier = Modifier
                                        .background(ComposeAppTheme.colors.lawrence)
                                        .testTag(if (pagedList) TestTags.TX_EMPTY else TestTags.ACTIVITY_EMPTY),
                                    text = stringResource(uiState.tab.emptyText),
                                    icon = R.drawable.ic_outgoingraw
                                )
                            }
                        } else {
                            val listState = rememberSaveable(
                                uiState.transactionListId,
                                (accountsViewModel.balanceScreenState as? BalanceScreenState.HasAccount)?.accountViewItem?.id,
                                saver = LazyListState.Saver
                            ) {
                                LazyListState(0, 0)
                            }

                            val onClick: (TransactionViewItem) -> Unit = remember {
                                {
                                    onTransactionClick(
                                        it,
                                        viewModel,
                                        navigation
                                    )
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ComposeAppTheme.colors.lawrence)
                            ) {
                                transactionList(
                                    transactionsMap = transactionItems,
                                    // Swap record rows have no on-chain transaction to fetch a rate for.
                                    willShow = { if (pagedList) viewModel.willShow(it) },
                                    onClick = onClick,
                                    // Only Transfers pages its on-chain transactions in.
                                    onBottomReached = { if (pagedList) viewModel.onBottomReached() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun onTransactionClick(
    transactionViewItem: TransactionViewItem,
    viewModel: TransactionsViewModel,
    navigation: HSNavigation
) {
    transactionViewItem.swapRecordId?.let { id ->
        navigation.slideFromRight(SwapInfoPage(SwapInfoPage.Input(id)))
        return
    }

    val transactionItem = viewModel.getTransactionItem(transactionViewItem) ?: return

    TransactionInfoPayload.record = transactionItem.record

    navigation.slideFromBottom(TransactionInfoPage)

    stat(page = StatPage.Transactions, event = StatEvent.Open(StatPage.TransactionInfo))
}

fun LazyListScope.transactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    transactionsMap.forEach { (dateHeader, transactions) ->
        stickyHeader {
            HeaderStick(
                borderBottom = true,
                text = dateHeader,
                color = ComposeAppTheme.colors.lawrence,
            )
        }

        items(transactions, key = { it.uid }) { item ->
            TransactionCell(item) { onClick.invoke(item) }

            willShow.invoke(item)

            if (item.uid == bottomReachedUid) {
                onBottomReached.invoke()
            }
        }
    }

    item {
        VSpacer(20.dp)
        VSpacer(70.dp)
    }
}

fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun TransactionCell(item: TransactionViewItem, onClick: () -> Unit) {
    // A transaction that did not complete shows no amount colour; its status line tells why.
    val ended = item.state == TransactionIconState.Failed ||
        item.state == TransactionIconState.Muted ||
        item.state == TransactionIconState.Warning
    val subtitleColor = when (item.state) {
        TransactionIconState.Pending -> pulsingTextColor()
        TransactionIconState.Failed -> ComposeAppTheme.colors.lucian
        TransactionIconState.Warning -> ComposeAppTheme.colors.jacob
        else -> ComposeAppTheme.colors.grey
    }

    val contentAlpha = if (item.spam) 0.5f else 1f

    Column {
        CellPrimary(
            modifier = Modifier.testTag(TestTags.txItem(item.uid)),
            backgroundColor = ComposeAppTheme.colors.lawrence,
            onClick = onClick,
            left = {
                TransactionIcon(
                    modifier = Modifier.alpha(contentAlpha),
                    icon = item.type.icon,
                    state = item.state,
                )
            },
            middle = {
                Column(modifier = Modifier.alpha(contentAlpha)) {
                    TransactionTitle(
                        modifier = Modifier.testTag(TestTags.txItemTitle(item.uid)),
                        title = item.title
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        modifier = Modifier.testTag(TestTags.txItemSubtitle(item.uid)),
                        text = item.subtitle,
                        // Without a line height the line box ends at the font's descent, which Manrope's g crosses and gets clipped.
                        style = ComposeAppTheme.typography.subhead.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                        color = subtitleColor,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            },
            right = {
                Column(
                    modifier = Modifier.alpha(contentAlpha),
                    horizontalAlignment = Alignment.End,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.primaryValue?.let { coloredValue ->
                            Text(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .testTag(TestTags.txItemValue(item.uid)),
                                text = if (item.showAmount) coloredValue.value else "* * *",
                                style = ComposeAppTheme.typography.headline2,
                                color = if (ended) ComposeAppTheme.colors.grey else coloredValue.color.compose(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                        if (item.doubleSpend) {
                            Image(
                                modifier = Modifier.padding(start = 6.dp),
                                painter = painterResource(R.drawable.ic_double_spend_20),
                                contentDescription = null
                            )
                        }
                        item.locked?.let { locked ->
                            Image(
                                modifier = Modifier.padding(start = 6.dp),
                                painter = painterResource(if (locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20),
                                contentDescription = null
                            )
                        }
                    }
                    Spacer(Modifier.height(1.dp))
                    item.secondaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "",
                            style = ComposeAppTheme.typography.subheadSB.copy(lineHeight = 20.sp),
                            color = if (ended) ComposeAppTheme.colors.andy else coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                }
            },
        )
        HsDivider()
    }
}

@Composable
private fun TransactionTitle(title: TransactionViewItem.Title, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (title) {
            is TransactionViewItem.Title.Single -> TransactionTitlePart(title, Modifier.weight(1f, fill = false))
            is TransactionViewItem.Title.Pair -> {
                TransactionTitlePart(title.from, Modifier.weight(1f, fill = false))
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                    painter = painterResource(R.drawable.arrow_m_right_24),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null
                )
                TransactionTitlePart(title.to, Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun TransactionTitlePart(part: TransactionViewItem.Title.Single, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        when (val icon = part.icon) {
            is TransactionViewItem.Icon.Regular -> HsImage(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .clip(if (icon.rectangle) RoundedCornerShape(CornerSize(2.dp)) else CircleShape),
                url = icon.url,
                alternativeUrl = icon.alternativeUrl,
                placeholder = icon.placeholder
            )

            // The network stands in for a coin here, so it keeps its square shape.
            is TransactionViewItem.Icon.Platform -> HsImage(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp)),
                url = icon.blockchainType.imageUrl,
                placeholder = R.drawable.coin_placeholder
            )

            null -> Unit
        }
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = part.text,
            style = ComposeAppTheme.typography.headline2,
            color = if (part.muted) ComposeAppTheme.colors.grey else ComposeAppTheme.colors.leah,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivityTabs(
    selectedTab: ActivityTab,
    onSelect: (ActivityTab) -> Unit
) {
    val tabs = ActivityTab.entries
    TabsFolder(
        tabs = tabs.map { TabFolderItem(stringResource(it.title), testTag = it.testTag) },
        selectedIndex = tabs.indexOf(selectedTab),
        onSelect = { onSelect(tabs[it]) },
    )
}

private val ActivityTab.testTag: String
    get() = when (this) {
        ActivityTab.Transfers -> TestTags.ACTIVITY_TAB_TRANSFER
        ActivityTab.Swaps -> TestTags.ACTIVITY_TAB_SWAP
        ActivityTab.Privates -> TestTags.ACTIVITY_TAB_PRIVATE_SEND
        ActivityTab.CrossPays -> TestTags.ACTIVITY_TAB_CROSS_PAY
    }

data class Filter<T>(val item: T, val selected: Boolean)
