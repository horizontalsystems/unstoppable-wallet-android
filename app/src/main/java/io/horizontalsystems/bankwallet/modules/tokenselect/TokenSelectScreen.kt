package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardInner2
import io.horizontalsystems.bankwallet.modules.balance.ui.BalanceCardSubtitleType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.FloatingSearchBarRow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.ListEmptyView
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
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

        Column(
            modifier = Modifier
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    top = paddingValues.calculateTopPadding(), // Keep top padding for the AppBar
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp // Explicitly ignore bottom padding from Scaffold's paddingValues
                )
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            val tabItems: List<TabItem<SelectChainTab>> = uiState.tabs.map { chainTab ->
                TabItem(
                    title = chainTab.title,
                    selected = chainTab == uiState.selectedTab,
                    item = chainTab,
                )
            }
            if (tabItems.isNotEmpty()) {
                ScrollableTabs(tabItems) {
                    viewModel.onTabSelected(it)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (uiState.noItems) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                            .background(ComposeAppTheme.colors.lawrence)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    // Hide keyboard when scrolling/tapping on list
                                    if (isSearchActive) {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                }
                            }
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
                                    onClickItem.invoke(item)
                                }
                            )
                            HsDivider()
                        }
                        item {
                            VSpacer(100.dp)
                        }
                    }
                }

                FloatingSearchBarRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    focusRequester = focusRequester,
                    keyboardController = keyboardController,
                    focusManager = focusManager,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.updateFilter(query)
                    }
                ) { isSearchActive = it }
            }
        }
    }
}