package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardSubtitleType
import io.horizontalsystems.bankwallet.ui.compose.ColoredTextStyle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_andy

@Composable
fun TokenSelectScreen(
    navController: NavController,
    title: String,
    onClickItem: (BalanceViewItem2) -> Unit,
    viewModel: TokenSelectViewModel,
    emptyItemsText: String,
    header: @Composable (() -> Unit)? = null
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = title,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
            )
        }
    ) { paddingValues ->
        val uiState = viewModel.uiState
        if (uiState.noItems) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                header?.invoke()
                ListEmptyView(
                    text = emptyItemsText,
                    icon = R.drawable.ic_empty_wallet
                )
            }
        } else {
            val tabItems: List<TabItem<SelectChainTab>> = uiState.tabs.map { chainTab ->
                TabItem(
                    title = chainTab.title,
                    selected = chainTab == uiState.selectedTab,
                    item = chainTab,
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SearchInput { text ->
                    viewModel.updateFilter(text)
                }
                if (tabItems.isNotEmpty()) {
                    ScrollableTabs(tabItems) {
                        viewModel.onTabSelected(it)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeAppTheme.colors.lawrence),
                    contentPadding = paddingValues
                ) {
                    item {
                        header?.invoke()
                    }
                    val balanceViewItems = uiState.items
                    items(balanceViewItems) { item ->
                        Box(
                            modifier = Modifier.clickable {
                                onClickItem.invoke(item)
                            }
                        ) {
                            BalanceCardInner(
                                viewItem = item,
                                type = BalanceCardSubtitleType.CoinName
                            )
                        }
                        HsDivider()
                    }
                    item {
                        VSpacer(32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    onSearchQueryChange: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeAppTheme.colors.blade)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = ComposeAppTheme.colors.andy,
                modifier = Modifier.size(24.dp)
            )

            HSpacer(12.dp)

            BasicTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    onSearchQueryChange(newValue)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        body_andy(
                            text = stringResource(R.string.Market_Search)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}