package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardSubtitleType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.SearchBar
import io.horizontalsystems.bankwallet.ui.compose.components.SectionUniversalItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TokenSelectScreen(
    navController: NavController,
    title: String,
    onClickItem: (BalanceViewItem2) -> Unit,
    viewModel: TokenSelectViewModel,
    emptyItemsText: String,
    header: @Composable (() -> Unit)? = null
) {
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                SearchBar(
                    title = title,
                    searchHintText = "",
                    menuItems = listOf(),
                    onClose = { navController.popBackStack() },
                    onSearchTextChanged = { text ->
                        viewModel.updateFilter(text)
                    }
                )
            }
        ) { paddingValues ->
            val uiState = viewModel.uiState
            if (uiState.noItems) {
                ListEmptyView(
                    text = emptyItemsText,
                    icon = R.drawable.ic_empty_wallet
                )
            } else {
                LazyColumn(contentPadding = paddingValues) {
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
}