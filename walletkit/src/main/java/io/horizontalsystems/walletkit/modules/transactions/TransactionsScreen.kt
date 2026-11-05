package io.horizontalsystems.walletkit.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoPage
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoPayload
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HeaderStick
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.HsImage
import io.horizontalsystems.walletkit.ui.compose.components.ListEmptyView
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.headline2_leah
import io.horizontalsystems.walletkit.uiv3.TestTags
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.TransactionIcon
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabItem
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsTop
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsTopType
import kotlinx.coroutines.delay

private const val SlideDurationMillis = 300L

@Composable
fun TransactionsScreen(
    navigation: HSNavigation,
    viewModel: TransactionsViewModel,
) {
    val accountsViewModel =
        viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
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
        title = stringResource(R.string.Transactions_Title),
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
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes = filterTypes,
                    onTransactionTypeClick = {
                        viewModel.setFilterTransactionType(it)

                        stat(
                            page = StatPage.Transactions,
                            event = StatEvent.SwitchTab(it.statTab)
                        )
                    }
                )
            }

            Crossfade(
                uiState.viewState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ComposeAppTheme.colors.lawrence),
                label = ""
            ) { viewState ->
                if (listVisible && viewState == ViewState.Success) {
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
                                        .testTag(TestTags.TX_EMPTY),
                                    text = stringResource(R.string.Transactions_EmptyList),
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
                                    willShow = { viewModel.willShow(it) },
                                    onClick = onClick,
                                    onBottomReached = { viewModel.onBottomReached() }
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
    val failed = item.state == TransactionIconState.Failed

    Column {
        RowUniversal(
            modifier = Modifier
                .height(72.dp)
                .testTag(TestTags.txItem(item.uid))
                .background(ComposeAppTheme.colors.lawrence)
                .clickable(onClick = onClick),
        ) {
            TransactionIcon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .alpha(if (item.spam) 0.5f else 1f),
                icon = item.type.icon,
                state = item.state,
            )
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .alpha(if (item.spam) 0.5f else 1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TransactionTitle(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                            .testTag(TestTags.txItemTitle(item.uid)),
                        title = item.title
                    )
                    item.primaryValue?.let { coloredValue ->
                        Text(
                            modifier = Modifier.testTag(TestTags.txItemValue(item.uid)),
                            text = if (item.showAmount) coloredValue.value else "* * *",
                            style = ComposeAppTheme.typography.headline2,
                            color = if (failed) ComposeAppTheme.colors.grey else coloredValue.color.compose(),
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
                Row {
                    val subtitleModifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag(TestTags.txItemSubtitle(item.uid))
                    Text(
                        text = item.subtitle,
                        modifier = subtitleModifier,
                        style = ComposeAppTheme.typography.subhead.copy(fontWeight= FontWeight.SemiBold),
                        color = if (failed) ComposeAppTheme.colors.lucian else ComposeAppTheme.colors.grey,
                    )
                    item.secondaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "",
                            style = ComposeAppTheme.typography.subheadSB,
                            color = if (failed) ComposeAppTheme.colors.grey else coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
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

            is TransactionViewItem.Icon.Platform -> HsImage(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .clip(CircleShape),
                url = icon.blockchainType.imageUrl,
                placeholder = R.drawable.coin_placeholder
            )

            null -> Unit
        }
        headline2_leah(
            modifier = Modifier.weight(1f, fill = false),
            text = part.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterTransactionType>>,
    onTransactionTypeClick: (FilterTransactionType) -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    TabsTop(TabsTopType.Scrolled, tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
