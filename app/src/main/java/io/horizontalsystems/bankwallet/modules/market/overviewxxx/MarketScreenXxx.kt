package cash.p.terminal.modules.market.overviewxxx

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.StatSection
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPage
import cash.p.terminal.modules.coin.CoinFragment
import cash.p.terminal.modules.market.MarketModule.Tab
import cash.p.terminal.modules.market.favorites.MarketFavoritesScreen
import cash.p.terminal.modules.market.posts.MarketPostsScreen
import cash.p.terminal.modules.market.topcoins.TopCoins
import cash.p.terminal.modules.market.topplatforms.TopPlatforms
import cash.p.terminal.modules.metricchart.MetricsType
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.ScrollableTabs
import cash.p.terminal.ui.compose.components.TabItem
import cash.p.terminal.ui.compose.components.caption_bran
import cash.p.terminal.ui.compose.components.caption_grey
import cash.p.terminal.ui.compose.components.caption_lucian
import cash.p.terminal.ui.compose.components.caption_remus

@Composable
fun MarketScreenXxx(navController: NavController) {
    val viewModel = viewModel<MarketViewModelXxx>(factory = MarketModuleXxx.Factory())
    val uiState = viewModel.uiState
    val tabs = viewModel.tabs

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Market_Title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Market_Search),
                        icon = R.drawable.icon_search,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = {
                            navController.slideFromRight(R.id.marketSearchFragment)

                            stat(
                                page = StatPage.Markets,
                                event = StatEvent.Open(StatPage.MarketSearch)
                            )
                        },
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Market_Filters),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = {
                            navController.slideFromRight(R.id.marketAdvancedSearchFragment)

                            stat(
                                page = StatPage.Markets,
                                event = StatEvent.Open(StatPage.AdvancedSearch)
                            )
                        },
                    ),
                )
            )
        }
    ) {
        Column(
            Modifier
                .padding(it)
                .background(ComposeAppTheme.colors.tyler)
        ) {
            MetricsBoard(navController, uiState.marketOverviewItems)
            Divider(
                color = ComposeAppTheme.colors.steel10,
                thickness = 1.dp
            )
            TabsSection(navController, tabs, uiState.selectedTab) { tab ->
                viewModel.onSelect(tab)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabsSection(
    navController: NavController,
    tabs: Array<Tab>,
    selectedTab: Tab,
    onTabClick: (Tab) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = selectedTab.ordinal) { tabs.size }

    LaunchedEffect(key1 = selectedTab, block = {
        pagerState.scrollToPage(selectedTab.ordinal)
    })
    val tabItems = tabs.map {
        TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
    }

    ScrollableTabs(tabItems) {
        onTabClick(it)
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false
    ) { page ->
        when (tabs[page]) {
            Tab.Coins -> {
                TopCoins(onCoinClick = { onCoinClick(it, navController) })
                stat(page = StatPage.MarketOverview, section = StatSection.TopGainers, event = StatEvent.Open(StatPage.TopCoins))
            }

            Tab.Watchlist -> MarketFavoritesScreen(navController)
            Tab.Posts -> MarketPostsScreen()
            Tab.Platform -> {
                TopPlatforms(navController)
                stat(page = StatPage.MarketOverview, event = StatEvent.Open(StatPage.TopPlatforms))
            }
            Tab.Pairs -> {
            }

            Tab.Sectors -> {
            }
        }
    }
}

@Composable
fun MetricsBoard(
    navController: NavController,
    marketOverviewItems: List<MarketModuleXxx.MarketOverviewViewItem>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ComposeAppTheme.colors.tyler)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        HSpacer(4.dp)
        marketOverviewItems.forEach { item ->
            Row(
                modifier = Modifier
                    .clickable {
                        openMetricsPage(item.metricsType, navController)
                    }
                    .padding(8.dp)
            ) {
                HSpacer(12.dp)
                caption_grey(text = item.title)
                HSpacer(4.dp)
                caption_bran(text = item.value)
                HSpacer(4.dp)
                if (item.changePositive) {
                    caption_remus(text = item.change)
                } else {
                    caption_lucian(text = item.change)
                }
                HSpacer(12.dp)
            }
        }
        HSpacer(4.dp)
    }
}

private fun openMetricsPage(metricsType: MetricsType, navController: NavController) {
    if (metricsType == MetricsType.TvlInDefi) {
        navController.slideFromBottom(R.id.tvlFragment)
    } else {
        navController.slideFromBottom(R.id.metricsPageFragment, metricsType)
    }

    stat(page = StatPage.MarketOverview, event = StatEvent.Open(metricsType.statPage))
}

private fun onCoinClick(coinUid: String, navController: NavController) {
    val arguments = CoinFragment.Input(coinUid)

    navController.slideFromRight(R.id.coinFragment, arguments)

    stat(page = StatPage.TopCoins, event = StatEvent.OpenCoin(coinUid))
}
