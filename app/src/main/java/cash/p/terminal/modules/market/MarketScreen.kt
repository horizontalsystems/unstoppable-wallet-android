package cash.p.terminal.modules.market

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.StatSection
import cash.p.terminal.modules.market.MarketModule.Tab
import cash.p.terminal.modules.market.favorites.MarketFavoritesScreen
import cash.p.terminal.modules.market.posts.MarketPostsScreen
import cash.p.terminal.modules.market.topcoins.TopCoins
import cash.p.terminal.modules.market.toppairs.TopPairsScreen
import cash.p.terminal.modules.market.topplatforms.TopPlatforms
import cash.p.terminal.modules.metricchart.MetricsType
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.ScrollableTabs
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.caption_bran
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.caption_lucian
import cash.p.terminal.ui_compose.components.caption_remus
import cash.p.terminal.ui_compose.components.micro_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.models.MarketGlobal
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

@Composable
fun MarketScreen(navController: NavController, paddingValuesParent: PaddingValues) {
    val viewModel = viewModel<MarketViewModel>(factory = MarketModule.Factory())
    val uiState = viewModel.uiState
    val tabs = viewModel.tabs

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
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
                        },
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Market_Filters),
                        icon = R.drawable.ic_manage_2_24,
                        onClick = {
                            navController.slideFromRight(R.id.marketAdvancedSearchFragment)
                        },
                    ),
                )
            )
        }
    ) {
        Column(
            Modifier
                .padding(
                    top = it.calculateTopPadding(),
                    bottom = paddingValuesParent.calculateBottomPadding()
                )
                .background(ComposeAppTheme.colors.tyler)
        ) {
            Crossfade(uiState.marketGlobal, label = "") {
                MetricsBoard(navController, it, uiState.currency)
            }
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
            }

            Tab.Watchlist -> {
                MarketFavoritesScreen(navController)
            }

            Tab.Posts -> {
                MarketPostsScreen()
            }

            Tab.Platform -> {
                TopPlatforms(navController)
            }

            Tab.Pairs -> {
                TopPairsScreen()
            }
        }
    }
}

private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
    return App.numberFormatter.formatFiatShort(value, symbol, 2)
}

private fun getDiff(it: BigDecimal): String {
    val sign = if (it >= BigDecimal.ZERO) "+" else "-"
    return App.numberFormatter.format(it.abs(), 0, 2, sign, "%")
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetricsBoard(
    navController: NavController,
    marketGlobal: MarketGlobal?,
    currency: Currency
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TotalMarketCap),
            value = marketGlobal?.marketCap,
            change = marketGlobal?.marketCapChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TotalMarketCap, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_Volume),
            value = marketGlobal?.volume,
            change = marketGlobal?.volumeChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Volume24h, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TvlInDefi),
            value = marketGlobal?.tvl,
            change = marketGlobal?.tvlChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TvlInDefi, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_EtfInflow),
            value = marketGlobal?.etfTotalInflow,
            change = marketGlobal?.etfDailyInflow,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Etf, navController)
            }
        )
    }
}

@Composable
private fun VDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color = ComposeAppTheme.colors.steel10)
    )
}

@Composable
private fun RowScope.MarketTotalCard(
    title: String,
    value: BigDecimal?,
    change: BigDecimal?,
    currency: Currency,
    onClick: () -> Unit,
) {
    val changeStr = change?.let { getDiff(it) }
    val changePositive = change?.let { it > BigDecimal.ZERO }
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(12.dp)
            .clickable(onClick = onClick)
    ) {
        micro_grey(
            text = title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        VSpacer(4.dp)
        caption_bran(
            text = value?.let { formatFiatShortened(it, currency.symbol) } ?: "---",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        VSpacer(4.dp)
        if (changePositive == null) {
            caption_grey(
                text = changeStr ?: "---",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        } else if (changePositive) {
            caption_remus(
                text = changeStr ?: "---",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        } else {
            caption_lucian(
                text = changeStr ?: "---",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

private fun openMetricsPage(metricsType: MetricsType, navController: NavController) {
    when (metricsType) {
        MetricsType.TvlInDefi -> {
            navController.slideFromBottom(R.id.tvlFragment)
        }

        MetricsType.Etf -> {
            navController.slideFromBottom(R.id.etfFragment)
        }

        else -> {
            navController.slideFromBottom(R.id.metricsPageFragment, metricsType)
        }
    }
}

private fun onCoinClick(coinUid: String, navController: NavController) {
    val arguments = CoinFragmentInput(coinUid)

    navController.slideFromRight(R.id.coinFragment, arguments)
}
