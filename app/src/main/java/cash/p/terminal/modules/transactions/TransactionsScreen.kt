package cash.p.terminal.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.modules.balance.BalanceAccountsViewModel
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceScreenState
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HSCircularProgressIndicator
import cash.p.terminal.ui_compose.components.HeaderStick
import cash.p.terminal.ui_compose.components.HsImage
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.ScrollableTabs
import cash.p.terminal.ui_compose.components.SectionUniversalItem
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.entities.SectionItemPosition
import cash.p.terminal.ui_compose.sectionItemBorder
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.entities.ViewState

@Composable
fun TransactionsScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: TransactionsViewModel,
    onShowAllTransactionsClicked: () -> Unit
) {
    val accountsViewModel =
        viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val showFilterAlertDot by viewModel.filterResetEnabled.observeAsState(false)

    val uiState = viewModel.uiState
    val syncing = uiState.syncing
    val transactions = uiState.transactions

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            AppBar(
                title = stringResource(R.string.Transactions_Title),
                showSpinner = syncing,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Transactions_Filter),
                        icon = R.drawable.ic_manage_2_24,
                        showAlertDot = showFilterAlertDot,
                        onClick = {
                            navController.slideFromRight(R.id.transactionFilterFragment)
                        },
                    )
                )
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes = filterTypes,
                    onTransactionTypeClick = {
                        viewModel.setFilterTransactionType(it)
                    }
                )
            }

            Crossfade(uiState.viewState, label = "") { viewState ->
                if (viewState == ViewState.Success) {
                    transactions?.let { transactionItems ->
                        if (transactionItems.isEmpty() && !uiState.hasHiddenTransactions) {
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

    viewModel.tmpItemToShow = transactionItem

    navController.slideFromBottom(R.id.transactionInfoFragment)
}

fun LazyListScope.transactionsHiddenBlock(
    shortBlock: Boolean,
    onShowAllTransactionsClicked: () -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!shortBlock) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = ComposeAppTheme.colors.raina,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        painter = painterResource(R.drawable.ic_eye_off),
                        contentDescription = "transactions hidden",
                        tint = ComposeAppTheme.colors.grey
                    )
                }
                Spacer(Modifier.height(32.dp))
                subhead2_grey(
                    text = stringResource(R.string.Transactions_Hide),
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                subhead2_grey(
                    text = "*****",
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(32.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                title = stringResource(R.string.show_all_transactions),
                onClick = onShowAllTransactionsClicked
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.transactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    transactionsMap.forEach { (dateHeader, transactions) ->
        stickyHeader {
            HeaderStick(text = dateHeader)
        }

        val itemsCount = transactions.size
        val singleElement = itemsCount == 1

        itemsIndexed(
            items = transactions,
            key = { _, item ->
                item.uid
            }
        ) { index, item ->
            val position: SectionItemPosition = when {
                singleElement -> SectionItemPosition.Single
                index == 0 -> SectionItemPosition.First
                index == itemsCount - 1 -> SectionItemPosition.Last
                else -> SectionItemPosition.Middle
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TransactionCell(item, position) { onClick.invoke(item) }
            }

            willShow.invoke(item)

            if (item.uid == bottomReachedUid) {
                onBottomReached.invoke()
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    item {
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun TransactionCell(item: TransactionViewItem, position: SectionItemPosition, onClick: () -> Unit) {
    val divider = position == SectionItemPosition.Middle || position == SectionItemPosition.Last
    SectionUniversalItem(
        borderTop = divider,
    ) {
        val clipModifier = when (position) {
            SectionItemPosition.First -> {
                Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            }

            SectionItemPosition.Last -> {
                Modifier.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            }

            SectionItemPosition.Single -> {
                Modifier.clip(RoundedCornerShape(12.dp))
            }

            else -> Modifier
        }

        val borderModifier = if (position != SectionItemPosition.Single) {
            Modifier.sectionItemBorder(1.dp, ComposeAppTheme.colors.steel20, 12.dp, position)
        } else {
            Modifier.border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
        }

        RowUniversal(
            modifier = Modifier
                .fillMaxSize()
                .then(clipModifier)
                .then(borderModifier)
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
                            painter = painterResource(icon.iconRes ?: R.drawable.coin_placeholder),
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

                    is TransactionViewItem.Icon.ImageResource -> {}
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
                            text = if (item.showAmount) coloredValue.value else "*****",
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
                            text = if (item.showAmount) coloredValue.value else "*****",
                            style = ComposeAppTheme.typography.subhead2,
                            color = coloredValue.color.compose(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
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

    ScrollableTabs(tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
