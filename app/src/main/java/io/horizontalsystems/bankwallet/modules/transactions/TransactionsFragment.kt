package io.horizontalsystems.bankwallet.modules.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.RotatingCircleProgressView
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class TransactionsFragment : BaseFragment() {

    private val viewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    TransactionsScreen(viewModel, findNavController())
                }
            }
        }
    }
}


@Composable
private fun TransactionsScreen(viewModel: TransactionsViewModel, navController: NavController) {
    val filterCoins by viewModel.filterCoinsLiveData.observeAsState()
    val filterTypes by viewModel.filterTypesLiveData.observeAsState()
    val transactions by viewModel.transactionList.observeAsState()
    val showSpinner by viewModel.syncingLiveData.observeAsState(false)
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.Transactions_Title),
                showSpinner = showSpinner
            )
            filterTypes?.let { filterTypes ->
                FilterTypeTabs(
                    filterTypes,
                    { viewModel.setFilterTransactionType(it) },
                    { scrollToTopAfterUpdate = true })
            }
            filterCoins?.let { filterCoins ->
                FilterCoinTabs(
                    filterCoins,
                    { viewModel.setFilterCoin(it) },
                    { scrollToTopAfterUpdate = true })
            }
            transactions?.let { transactionItems ->
                if (transactionItems.isEmpty()) {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 48.dp)
                                .align(Alignment.Center),
                            text = stringResource(id = R.string.Transactions_EmptyList),
                            textAlign = TextAlign.Center,
                            color = ComposeAppTheme.colors.grey,
                            style = ComposeAppTheme.typography.subhead2,
                        )
                    }
                } else {
                    TransactionList(
                        transactionItems,
                        scrollToTopAfterUpdate,
                        { viewModel.willShow(it) },
                        { onTransactionClick(it, viewModel, navController) },
                        { viewModel.onBottomReached() }
                    )
                    if (scrollToTopAfterUpdate) {
                        scrollToTopAfterUpdate = false
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

    val navOptionsFromBottom = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_from_bottom)
        .setExitAnim(R.anim.slide_to_top)
        .setPopEnterAnim(R.anim.slide_from_top)
        .setPopExitAnim(R.anim.slide_to_bottom)
        .build()

    navController.navigate(
        R.id.mainFragment_to_transactionInfoFragment, null, navOptionsFromBottom
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(
    transactionsMap: Map<String, List<TransactionViewItem>>,
    scrollToTop: Boolean,
    willShow: (TransactionViewItem) -> Unit,
    onClick: (TransactionViewItem) -> Unit,
    onBottomReached: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val bottomReachedUid = getBottomReachedUid(transactionsMap)

    LazyColumn(state = listState) {
        transactionsMap.forEach { (dateHeader, transactions) ->
            stickyHeader {
                DateHeader(dateHeader)
            }

            items(transactions) { item ->
                TransactionCell(item) { onClick.invoke(item) }

                willShow.invoke(item)

                if (item.uid == bottomReachedUid) {
                    onBottomReached.invoke()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (scrollToTop) {
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
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
    Header(borderTop = false, borderBottom = true) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = dateHeader,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead1,
            maxLines = 1,
        )
    }
}

@Composable
fun TransactionCell(item: TransactionViewItem, onClick: () -> Unit) {
    CellMultilineClear(borderBottom = true, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(52.dp).fillMaxHeight()) {
                item.progress?.let { progress ->
                    AndroidView(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(41.dp),
                        factory = { context ->
                            RotatingCircleProgressView(context)
                        },
                        update = { view ->
                            view.setProgressColored(
                                progress,
                                view.context.getColor(R.color.grey_50),
                                true
                            )
                        }
                    )
                }
                Image(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(item.typeIcon),
                    contentDescription = null
                )
            }
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Row {
                    Text(
                        text = item.title,
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.body,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.primaryValue?.let { coloredValue ->
                        ContentColored(colorName = coloredValue.color) {
                            Text(
                                text = coloredValue.value,
                                style = ComposeAppTheme.typography.headline2,
                                maxLines = 1,
                            )
                        }
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
                            painter = painterResource(R.drawable.ic_incoming_20),
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(1.dp))
                Row {
                    Text(
                        text = item.subtitle,
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead2,
                        maxLines = 1,
                    )
                    Spacer(Modifier.weight(1f))
                    item.secondaryValue?.let { coloredValue ->
                        ContentColored(colorName = coloredValue.color) {
                            Text(
                                text = coloredValue.value,
                                style = ComposeAppTheme.typography.subhead2,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTypeTabs(
    filterTypes: List<Filter<FilterTransactionType>>,
    onTransactionTypeClick: (FilterTransactionType) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterTypes.map {
        TabItem(stringResource(it.item.title), it.selected, it.item)
    }

    ScrollableTabs(tabItems) { transactionType ->
        onTransactionTypeClick.invoke(transactionType)
        scrollToTopAfterUpdate.invoke()
    }
}

@Composable
private fun FilterCoinTabs(
    filterCoins: List<Filter<TransactionWallet>>,
    onCoinFilterClick: (TransactionWallet?) -> Unit,
    scrollToTopAfterUpdate: () -> Unit
) {
    val tabItems = filterCoins.mapNotNull {
        it.item.platformCoin?.let { platformCoin ->
            TabItem(
                platformCoin.code,
                it.selected,
                it.item,
                ImageSource.Remote(
                    platformCoin.coin.iconUrl,
                    platformCoin.coinType.iconPlaceholder
                ),
                it.item.badge
            )
        }
    }

    CardTabs(tabItems = tabItems, edgePadding = 16.dp) {
        onCoinFilterClick.invoke(it)
        scrollToTopAfterUpdate.invoke()
    }
}

data class Filter<T>(val item: T, val selected: Boolean)
