package io.horizontalsystems.bankwallet.modules.market

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketModule.Tab
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesScreen
import io.horizontalsystems.bankwallet.modules.market.posts.MarketPostsScreen
import io.horizontalsystems.bankwallet.modules.market.topcoins.TopCoins
import io.horizontalsystems.bankwallet.modules.market.toppairs.TopPairsScreen
import io.horizontalsystems.bankwallet.modules.market.topplatforms.TopPlatforms
import io.horizontalsystems.bankwallet.modules.market.topsectors.TopSectorsScreen
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.caption_bran
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.caption_remus
import io.horizontalsystems.bankwallet.ui.compose.components.micro_grey
import io.horizontalsystems.marketkit.models.MarketGlobal
import java.math.BigDecimal

@Composable
fun MarketScreen(navController: NavController) {
    val viewModel = viewModel<MarketViewModel>(factory = MarketModule.Factory())
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

        stat(page = StatPage.Markets, event = StatEvent.SwitchTab(selectedTab.statTab))
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
            Tab.Coins -> TopCoins(onCoinClick = { onCoinClick(it, navController) })
            Tab.Watchlist -> MarketFavoritesScreen(navController)
            Tab.Posts -> MarketPostsScreen()
            Tab.Platform -> TopPlatforms(navController)
            Tab.Pairs -> TopPairsScreen()
            Tab.Sectors -> TopSectorsScreen(navController)
        }
    }
}

private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
    return App.numberFormatter.formatFiatShort(value, symbol, 2)
}

private fun getDiff(it: BigDecimal): String {
    return App.numberFormatter.format(it.abs(), 0, 2, "", "%")
}

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
            changePercentage = marketGlobal?.marketCapChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TotalMarketCap, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_Volume),
            value = marketGlobal?.volume,
            changePercentage = marketGlobal?.volumeChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Volume24h, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TvlInDefi),
            value = marketGlobal?.tvl,
            changePercentage = marketGlobal?.tvlChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TvlInDefi, navController)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_EtfInflow),
            value = marketGlobal?.etfTotalInflow,
            changeFiat = marketGlobal?.etfDailyInflow,
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
    changePercentage: BigDecimal? = null,
    changeFiat: BigDecimal? = null,
    currency: Currency,
    onClick: () -> Unit,
) {
    val changeStr: String?
    val changePositive: Boolean?

    if (changePercentage != null) {
        changeStr = getDiff(changePercentage)
        changePositive = changePercentage > BigDecimal.ZERO
    } else if (changeFiat != null) {
        changeStr = formatFiatShortened(changeFiat, currency.symbol)
        changePositive = changeFiat > BigDecimal.ZERO
    } else {
        changeStr = null
        changePositive = null
    }

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

        if (changeStr == null || changePositive == null) {
            caption_grey(
                text = "---",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        } else if (changePositive) {
            caption_remus(
                text = "+$changeStr",
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        } else {
            caption_lucian(
                text = "-$changeStr",
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

    stat(page = StatPage.Markets, event = StatEvent.Open(metricsType.statPage))
}

private fun onCoinClick(coinUid: String, navController: NavController) {
    val arguments = CoinFragment.Input(coinUid)

    navController.slideFromRight(R.id.coinFragment, arguments)

    stat(page = StatPage.Markets, section = StatSection.Coins, event = StatEvent.OpenCoin(coinUid))
}
