package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.BalanceAccountsViewModel
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceScreenState
import io.horizontalsystems.bankwallet.modules.settings.about.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsViewModel
) {
    val accountsViewModel = viewModel<BalanceAccountsViewModel>(factory = BalanceModule.AccountsFactory())

    val filterCoins by viewModel.filterCoinsLiveData.observeAsState()
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val filterBlockchains by viewModel.filterBlockchainsLiveData.observeAsState()
    val transactions by viewModel.transactionList.observeAsState()
    val viewState by viewModel.viewState.observeAsState()
    val syncing by viewModel.syncingLiveData.observeAsState(false)
    val filterResetEnabled by viewModel.filterResetEnabled.collectAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = TranslatableString.ResString(R.string.Transactions_Title),
                showSpinner = syncing,
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Reset),
                        enabled = filterResetEnabled,
                        onClick = {
                            viewModel.resetFilters()
                        }
                    )
                )
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes = filterTypes,
                    onTransactionTypeClick = viewModel::setFilterTransactionType
                )
            }
            filterBlockchains?.let { filterBlockchains ->
                CellHeaderSorting(borderBottom = true) {
                    var showFilterBlockchainDialog by remember { mutableStateOf(false) }
                    if (showFilterBlockchainDialog) {
                        SelectorDialogCompose(
                            title = stringResource(R.string.Transactions_Filter_Blockchain),
                            items = filterBlockchains.map {
                                TabItem(it.item?.name ?: stringResource(R.string.Transactions_Filter_AllBlockchains), it.selected, it)
                            },
                            onDismissRequest = {
                                showFilterBlockchainDialog = false
                            },
                            onSelectItem = viewModel::onEnterFilterBlockchain
                        )
                    }

                    val filterBlockchain = filterBlockchains.firstOrNull { it.selected }?.item
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ButtonSecondaryTransparent(
                            title = filterBlockchain?.name ?: stringResource(R.string.Transactions_Filter_AllBlockchains),
                            iconRight = R.drawable.ic_down_arrow_20,
                            onClick = {
                                showFilterBlockchainDialog = true
                            }
                        )

                        filterCoins?.let { filterCoins ->
                            val filterCoin = filterCoins.find { it.selected }?.item

                            val coinCode = filterCoin?.token?.coin?.code
                            val badge = filterCoin?.badge
                            val title = when {
                                badge != null -> "$coinCode ($badge)"
                                else -> coinCode
                            }

                            ButtonSecondaryTransparent(
                                title = title ?: stringResource(R.string.Transactions_Filter_AllCoins),
                                iconRight = R.drawable.ic_down_arrow_20,
                                onClick = {
                                    navController.slideFromBottom(R.id.filterCoinFragment)
                                }
                            )
                        }
                    }
                }
            }

            Crossfade(viewState) { viewState ->
                when (viewState) {
                    ViewState.Success -> {
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
                                val filterCoin = filterCoins?.find { it.selected }?.item
                                val filterType = filterTypes?.find { it.selected }?.item
                                val filterBlockchain = filterBlockchains?.find { it.selected }?.item

                                val listState = rememberSaveable(
                                    filterCoin,
                                    filterType,
                                    filterBlockchain,
                                    (accountsViewModel.balanceScreenState as? BalanceScreenState.HasAccount)?.accountViewItem?.id,
                                    saver = LazyListState.Saver
                                ) {
                                    LazyListState(0, 0)
                                }

                                TransactionList(
                                    listState = listState,
                                    transactionsMap = transactionItems,
                                    willShow = { viewModel.willShow(it) },
                                    onClick = { onTransactionClick(it, viewModel, navController) },
                                    onBottomReached = { viewModel.onBottomReached() }
                                )
                            }
                        }
                    }
                    is ViewState.Error,
                    ViewState.Loading,
                    null -> {}
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    listState: LazyListState = rememberLazyListState(),
    transactionsMap: Map<String, List<TransactionViewItem>>,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    LazyColumn(state = listState) {
        transactionsMap.forEach { (dateHeader, transactions) ->
            stickyHeader {
                DateHeader(dateHeader)
            }

            val itemsCount = transactions.size
            val singleElement = itemsCount == 1

            itemsIndexed(transactions) { index, item ->
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
}

private fun getBottomReachedUid(transactionsMap: Map<String, List<TransactionViewItem>>): String? {
    val txList = transactionsMap.values.flatten()
    //get index not exact bottom but near to the bottom, to make scroll smoother
    val index = if (txList.size > 4) txList.size - 4 else 0

    return txList.getOrNull(index)?.uid
}

@Composable
fun DateHeader(dateHeader: String) {
    HeaderSorting {
        subhead1_grey(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = dateHeader,
            maxLines = 1,
        )
    }
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
                    .size(42.dp),
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
                        val shape = if (icon.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        CoinImage(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(shape),
                            iconUrl = icon.url,
                            placeholder = icon.placeholder
                        )
                    }
                    is TransactionViewItem.Icon.Double -> {
                        val backShape = if (icon.back.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        val frontShape = if (icon.front.rectangle) RoundedCornerShape(CornerSize(4.dp)) else CircleShape
                        CoinImage(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 4.dp, start = 6.dp)
                                .size(24.dp)
                                .clip(backShape),
                            iconUrl = icon.back.url,
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

                        CoinImage(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 4.dp, end = 6.dp)
                                .size(24.dp)
                                .clip(frontShape),
                            iconUrl = icon.front.url,
                            placeholder = icon.front.placeholder,
                        )
                    }
                    is TransactionViewItem.Icon.ImageResource -> {}
                }
            }
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Row {
                    body_leah(
                        modifier = Modifier.padding(end = 32.dp),
                        text = item.title,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.primaryValue?.let { coloredValue ->
                        Text(
                            text = coloredValue.value,
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
                            text = coloredValue.value,
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
