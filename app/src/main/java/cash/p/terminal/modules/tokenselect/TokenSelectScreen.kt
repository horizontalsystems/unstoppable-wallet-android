package cash.p.terminal.modules.tokenselect

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.balance.ui.BalanceCardInner
import cash.p.terminal.modules.balance.ui.BalanceCardSubtitleType
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui.compose.components.SearchBarV2
import cash.p.terminal.ui_compose.components.SectionUniversalItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun TokenSelectScreen(
    navController: NavController,
    title: String,
    uiState: TokenSelectUiState,
    searchHintText: String = "",
    onClickItem: (BalanceViewItem2) -> Unit,
    updateFilter: (String) -> Unit,
    emptyItemsText: String,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    header: @Composable (() -> Unit)? = null
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            SearchBarV2(
                title = title,
                searchHintText = searchHintText,
                menuItems = listOf(),
                onClose = { navController.popBackStack() },
                onSearchTextChanged = updateFilter
            )
        }
    ) { paddingValues ->
        if (uiState.noItems) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                header?.invoke()
                ListEmptyView(
                    text = emptyItemsText,
                    icon = R.drawable.ic_empty_wallet
                )
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.windowInsetsPadding(windowInsets)
            ) {
                item {
                    if (header == null) {
                        VSpacer(12.dp)
                    }
                    header?.invoke()
                }
                val balanceViewItems = uiState.items
                itemsIndexed(balanceViewItems) { index, item ->
                    val lastItem = index == balanceViewItems.size - 1

                    Box(
                        modifier = Modifier.clickable {
                            onClickItem.invoke(item)
                        }
                    ) {
                        SectionUniversalItem(
                            borderTop = true,
                            borderBottom = lastItem
                        ) {
                            BalanceCardInner(
                                viewItem = item,
                                type = BalanceCardSubtitleType.CoinName
                            )
                        }
                    }
                }
                item {
                    VSpacer(32.dp)
                }
            }
        }
    }
}
