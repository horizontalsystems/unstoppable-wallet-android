package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderStick
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItemLoading
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType

@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsViewModel
) {
    val accountsViewModel =
        viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val showFilterAlertDot by viewModel.filterResetEnabled.observeAsState(false)

    val uiState = viewModel.uiState
    val syncing = uiState.syncing
    val transactions = uiState.transactions

    HSScaffold(
        title = stringResource(R.string.Transactions_Title),
        menuItems = buildList {
            if (syncing) {
                add(MenuItemLoading)
            }
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Transactions_Filter),
                    icon = R.drawable.ic_manage_2_24,
                    showAlertDot = showFilterAlertDot,
                    onClick = {
                        navController.slideFromRight(R.id.transactionFilterFragment)

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

            Crossfade(uiState.viewState, label = "") { viewState ->
                if (viewState == ViewState.Success) {
                    transactions?.let { transactionItems ->
                        if (transactionItems.isEmpty()) {
                            if (syncing) {
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
                                        navController
                                    )
                                }
                            }

                            LazyColumn(state = listState) {
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
    navController: NavController
) {
    val transactionItem = viewModel.getTransactionItem(transactionViewItem) ?: return

    viewModel.tmpTransactionRecordToShow = transactionItem.record

    navController.slideFromBottom(R.id.transactionInfoFragment)

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
        Spacer(modifier = Modifier.height(20.dp))
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

    Column {
        RowUniversal(
            modifier = Modifier
                .height(72.dp)
                .background(ComposeAppTheme.colors.lawrence)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(42.dp)
                    .alpha(if (item.spam) 0.5f else 1f),
                contentAlignment = Alignment.Center
            ) {
                item.progress?.let { progress ->
                    HSCircularProgressIndicator(progress)
                }
                val icon = item.icon
                when (icon) {
                    TransactionViewItem.Icon.Failed -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_attention_24),
                            tint = ComposeAppTheme.colors.lucian,
                            contentDescription = null
                        )
                    }

                    is TransactionViewItem.Icon.Platform -> {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(
                                icon.iconRes ?: R.drawable.coin_placeholder
                            ),
                            tint = ComposeAppTheme.colors.leah,
                            contentDescription = null
                        )
                    }

                    is TransactionViewItem.Icon.Regular -> {
                        val shape =
                            if (icon.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        HsImage(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(shape),
                            url = icon.url,
                            alternativeUrl = icon.alternativeUrl,
                            placeholder = icon.placeholder
                        )
                    }

                    is TransactionViewItem.Icon.Double -> {
                        val backShape =
                            if (icon.back.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        val frontShape =
                            if (icon.front.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        HsImage(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 4.dp, start = 6.dp)
                                .size(24.dp)
                                .clip(backShape),
                            url = icon.back.url,
                            alternativeUrl = icon.back.alternativeUrl,
                            placeholder = icon.back.placeholder,
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 4.5.dp, end = 6.5.dp)
                                .size(24.dp)
                                .clip(frontShape)
                                .background(ComposeAppTheme.colors.tyler)
                        )

                        HsImage(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 4.dp, end = 6.dp)
                                .size(24.dp)
                                .clip(frontShape),
                            url = icon.front.url,
                            alternativeUrl = icon.front.alternativeUrl,
                            placeholder = icon.front.placeholder,
                        )
                    }

                    is TransactionViewItem.Icon.ImageResource -> {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(icon.resourceId),
                            tint = ComposeAppTheme.colors.leah,
                            contentDescription = null
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .alpha(if (item.spam) 0.5f else 1f)
            ) {
                Row {
                    body_leah(
                        modifier = Modifier.padding(end = 32.dp),
                        text = item.title,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.primaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "* * *",
                            style = ComposeAppTheme.typography.body,
                            color = coloredValue.color.compose(),
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
                    if (item.sentToSelf) {
                        Image(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = painterResource(R.drawable.ic_arrow_return_20),
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row {
                    subhead2_grey(
                        text = item.subtitle,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 2,
                    )
                    item.secondaryValue?.let { coloredValue ->
                        Text(
                            text = if (item.showAmount) coloredValue.value else "",
                            style = ComposeAppTheme.typography.subheadR,
                            color = coloredValue.color.compose(),
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
