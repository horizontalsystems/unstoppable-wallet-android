package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardSubtitleType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottom.BottomSearchBar
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TokenSelectScreen(
    navController: NavController,
    title: String,
    onClickItem: (BalanceViewItem2) -> Unit,
    viewModel: TokenSelectViewModel,
    header: @Composable (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState

    val lazyListState = rememberSaveable(
        uiState.items.size,
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            if (isSearchActive) {
                isSearchActive = false
            }
        }
    }

    HSScaffold(
        title = title,
        onBack = { navController.popBackStack() },
    ) {

        Column {
            val tabItems: List<TabItem<SelectChainTab>> = uiState.tabs.map { chainTab ->
                TabItem(
                    title = chainTab.title,
                    selected = chainTab == uiState.selectedTab,
                    item = chainTab,
                )
            }
            if (tabItems.isNotEmpty()) {
                TabsTop(TabsTopType.Scrolled, tabItems) {
                    viewModel.onTabSelected(it)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (uiState.noItems) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ComposeAppTheme.colors.lawrence),
                    ) {
                        header?.invoke()
                        ListEmptyView(
                            text = stringResource(if (uiState.hasAssets) R.string.Search_NotFounded else R.string.Balance_NoAssetsToSend),
                            icon = R.drawable.warning_filled_24
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .background(ComposeAppTheme.colors.lawrence),
                        state = lazyListState
                    ) {
                        item {
                            header?.invoke()
                        }
                        val balanceViewItems = uiState.items
                        items(balanceViewItems) { item ->
                            BalanceCardInner2(
                                viewItem = item,
                                type = BalanceCardSubtitleType.CoinName,
                                onClick = {
                                    isSearchActive = false
                                    coroutineScope.launch {
                                        delay(200)
                                        onClickItem.invoke(item)
                                    }
                                }
                            )
                            HsDivider()
                        }
                        item {
                            VSpacer(100.dp)
                        }
                    }
                }

                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.updateFilter(query)
                    },
                )
            }
        }
    }
}