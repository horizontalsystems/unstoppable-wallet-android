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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statPage
import io.horizontalsystems.bankwallet.core.stats.statTab
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.coin.CoinScreen
import io.horizontalsystems.bankwallet.modules.market.MarketModule.Tab
import io.horizontalsystems.bankwallet.modules.market.earn.MarketEarnScreen
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesScreen
import io.horizontalsystems.bankwallet.modules.market.posts.MarketPostsScreen
import io.horizontalsystems.bankwallet.modules.market.topcoins.TopCoins
import io.horizontalsystems.bankwallet.modules.market.toppairs.TopPairsScreen
import io.horizontalsystems.bankwallet.modules.metricchart.MetricsType
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_bran
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.caption_lucian
import io.horizontalsystems.bankwallet.ui.compose.components.caption_remus
import io.horizontalsystems.bankwallet.ui.compose.components.micro_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTop
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabsTopType
import io.horizontalsystems.marketkit.models.MarketGlobal
import java.math.BigDecimal

@Composable
fun MarketScreen(backStack: NavBackStack<HSScreen>) {
    val viewModel = viewModel<MarketViewModel>(factory = MarketModule.Factory())
    val uiState = viewModel.uiState
    val tabs = viewModel.tabs

    HSScaffold(
        title = stringResource(R.string.Market_Title),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column() {
                Crossfade(uiState.marketGlobal, label = "") {
                    MetricsBoard(backStack, it, uiState.currency)
                }
                TabsSection(backStack, tabs, uiState.selectedTab) { tab ->
                    viewModel.onSelect(tab)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 72.dp),//bottomBar height 56 + 16 padding
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(ComposeAppTheme.colors.blade)
                        .height(48.dp)
                        .clickable {
//                            TODO("xxx nav3")
//                            navController.slideFromBottom(R.id.marketSearchFragment)
//                            stat(
//                                page = StatPage.Markets,
//                                event = StatEvent.Open(StatPage.MarketSearch)
//                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = ComposeAppTheme.colors.grey,
                        modifier = Modifier.size(24.dp)
                    )
                    HSpacer(8.dp)
                    body_grey(
                        text = stringResource(R.string.Balance_ReceiveHint_Search),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabsSection(
    backStack: NavBackStack<HSScreen>,
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
        TabItem(
            title = stringResource(id = it.titleResId),
            selected = it == selectedTab,
            item = it,
            premium = it.premium
        )
    }

    TabsTop(TabsTopType.Scrolled, tabItems) {
        onTabClick(it)
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeAppTheme.colors.lawrence)
    ) { page ->
        when (tabs[page]) {
            Tab.Coins -> {
                TopCoins(
                    onCoinClick = {
                        onCoinClick(it, backStack)
                    }
                )
            }
            Tab.Watchlist -> {
                MarketFavoritesScreen(backStack)
            }
            Tab.Earn -> {
                MarketEarnScreen(backStack)
            }
            Tab.Posts -> {
                MarketPostsScreen()
            }
            Tab.Platform -> {
//                TODO("xxx nav3")
//                TopPlatforms(navController)
            }
            Tab.Pairs -> TopPairsScreen()
            Tab.Sectors -> {
//                TODO("xxx nav3")
//                TopSectorsScreen(navController)
            }
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
    backStack: NavBackStack<HSScreen>,
    marketGlobal: MarketGlobal?,
    currency: Currency
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TotalMarketCapShort),
            value = marketGlobal?.marketCap,
            changePercentage = marketGlobal?.marketCapChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TotalMarketCap, backStack)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_VolumeShort),
            value = marketGlobal?.volume,
            changePercentage = marketGlobal?.volumeChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Volume24h, backStack)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TvlInDefi),
            value = marketGlobal?.tvl,
            changePercentage = marketGlobal?.tvlChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TvlInDefi, backStack)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_EtfInflow),
            value = marketGlobal?.etfTotalInflow,
            changeFiat = marketGlobal?.etfDailyInflow,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Etf, backStack)
            }
        )
    }
}

@Composable
private fun VDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(color = ComposeAppTheme.colors.blade)
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
        changeStr = formatFiatShortened(changeFiat.abs(), currency.symbol)
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

private fun openMetricsPage(metricsType: MetricsType, backStack: NavBackStack<HSScreen>) {
    when (metricsType) {
        MetricsType.TvlInDefi -> {
//            TODO("xxx nav3")
//            navController.slideFromBottom(R.id.tvlFragment)
        }

        MetricsType.Etf -> {
//            TODO("xxx nav3")
//            navController.slideFromBottom(R.id.etfFragment)
        }

        else -> {
//            TODO("xxx nav3")
//            navController.slideFromBottom(R.id.metricsPageFragment, metricsType)
        }
    }

    stat(page = StatPage.Markets, event = StatEvent.Open(metricsType.statPage))
}

private fun onCoinClick(coinUid: String, backStack: NavBackStack<HSScreen>) {
    backStack.add(CoinScreen(coinUid))

    stat(page = StatPage.Markets, event = StatEvent.OpenCoin(coinUid), section = StatSection.Coins)
}
