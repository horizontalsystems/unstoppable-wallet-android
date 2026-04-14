package com.quantum.wallet.bankwallet.modules.managewallets

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.slideFromRightForResult
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import com.quantum.wallet.bankwallet.modules.restoreconfig.BirthdayHeightConfig
import com.quantum.wallet.bankwallet.modules.tokenselect.SelectChainTab
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.ListEmptyView
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.bottom.BottomSearchBar
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellLeftImage
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightControlsSwitcher
import com.quantum.wallet.bankwallet.uiv3.components.cell.ImageType
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabItem
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTop
import com.quantum.wallet.bankwallet.uiv3.components.tabs.TabsTopType
import io.horizontalsystems.marketkit.models.Token

class ManageWalletsFragment : BaseComposeFragment() {

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }

    @Composable
    override fun GetContent(navController: NavController) {
        ManageWalletsScreen(
            navController,
            viewModel,
            restoreSettingsViewModel
        )
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ManageWalletsScreen(
    navController: NavController,
    viewModel: ManageWalletsViewModel,
    restoreSettingsViewModel: RestoreSettingsViewModel
) {
    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var isSearchActive by remember { mutableStateOf(false) }

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

    restoreSettingsViewModel.openBirthdayHeightConfig?.let { token ->
        restoreSettingsViewModel.birthdayHeightConfigOpened()

        navController.slideFromRightForResult<BirthdayHeightConfig.Result>(
            resId = R.id.zcashConfigure,
            input = token
        ) {
            if (it.config != null) {
                restoreSettingsViewModel.onEnter(it.config)
            } else {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }
        }

        stat(page = StatPage.CoinManager, event = StatEvent.Open(StatPage.BirthdayInput))
    }

    HSScaffold(
        title = stringResource(id = R.string.ManageCoins_title),
        onBack = { navController.popBackStack() },
        menuItems = if (viewModel.addTokenEnabled) {
            listOf(
                MenuItem(
                    title = TranslatableString.ResString(R.string.ManageCoins_AddToken),
                    icon = R.drawable.ic_add_24,
                    onClick = {
                        navController.slideFromRight(R.id.addTokenFragment)

                        stat(
                            page = StatPage.CoinManager,
                            event = StatEvent.Open(StatPage.AddToken)
                        )
                    }
                ))
        } else {
            listOf()
        },
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
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                if (uiState.items.isEmpty()) {
                    ListEmptyView(
                        text = stringResource(R.string.Search_NotFounded),
                        icon = R.drawable.warning_filled_24
                    )
                } else {
                    LazyColumn(
                        state = lazyListState
                    ) {
                        items(uiState.items) { viewItem ->
                            CoinCell(
                                viewItem = viewItem,
                                onItemClick = {
                                    if (viewItem.enabled) {
                                        viewModel.disable(viewItem.item)

                                        stat(
                                            page = StatPage.CoinManager,
                                            event = StatEvent.DisableToken(viewItem.item)
                                        )
                                    } else {
                                        viewModel.enable(viewItem.item)

                                        stat(
                                            page = StatPage.CoinManager,
                                            event = StatEvent.EnableToken(viewItem.item)
                                        )
                                    }
                                },
                                onInfoClick = {
                                    navController.slideFromBottom(
                                        R.id.configuredTokenInfo,
                                        viewItem.item
                                    )

                                    stat(
                                        page = StatPage.CoinManager,
                                        event = StatEvent.OpenTokenInfo(viewItem.item)
                                    )
                                }
                            )
                            HsDivider()
                        }
                        item {
                            VSpacer(88.dp)
                        }
                    }
                }
                BottomSearchBar(
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    onActiveChange = { active ->
                        isSearchActive = active
                    },
                    onSearchQueryChange = { query ->
                        viewModel.updateFilter(query)
                        searchQuery = query
                    }
                )
            }
        }
    }
}

@Composable
private fun CoinCell(
    viewItem: CoinViewItem<Token>,
    onItemClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    CellPrimary(
        left = {
            CellLeftImage(
                painter = viewItem.imageSource.painter(),
                type = ImageType.Ellipse,
                size = 32
            )
        },
        middle = {
            CellMiddleInfo(
                title = viewItem.title.hs,
                badge = viewItem.label?.hs,
                subtitle = viewItem.subtitle.hs,
            )
        },
        right = {
            CellRightControlsSwitcher(
                checked = viewItem.enabled,
                onInfoClick = if (viewItem.hasInfo) onInfoClick else null,
                onCheckedChange = { onItemClick.invoke() }
            )
        },
        onClick = onItemClick
    )
}
