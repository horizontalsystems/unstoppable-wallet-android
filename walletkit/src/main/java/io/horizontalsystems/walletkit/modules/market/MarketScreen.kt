package io.horizontalsystems.walletkit.modules.market

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.StatSection
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.core.stats.statPage
import io.horizontalsystems.walletkit.core.stats.statTab
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.modules.coin.CoinPage
import io.horizontalsystems.walletkit.modules.market.MarketModule.Tab
import io.horizontalsystems.walletkit.modules.market.earn.MarketEarnScreen
import io.horizontalsystems.walletkit.modules.market.etf.EtfPage
import io.horizontalsystems.walletkit.modules.market.favorites.MarketFavoritesScreen
import io.horizontalsystems.walletkit.modules.market.metricspage.MetricsPage
import io.horizontalsystems.walletkit.modules.market.posts.MarketPostsScreen
import io.horizontalsystems.walletkit.modules.market.search.MarketSearchPage
import io.horizontalsystems.walletkit.modules.market.topcoins.TopCoins
import io.horizontalsystems.walletkit.modules.market.toppairs.TopPairsScreen
import io.horizontalsystems.walletkit.modules.market.topplatforms.TopPlatforms
import io.horizontalsystems.walletkit.modules.market.topsectors.TopSectorsScreen
import io.horizontalsystems.walletkit.modules.market.tvl.TvlPage
import io.horizontalsystems.walletkit.modules.metricchart.MetricsType
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.caption_bran
import io.horizontalsystems.walletkit.ui.compose.components.caption_grey
import io.horizontalsystems.walletkit.ui.compose.components.caption_lucian
import io.horizontalsystems.walletkit.ui.compose.components.caption_remus
import io.horizontalsystems.walletkit.ui.compose.components.micro_grey
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabItem
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsTop
import io.horizontalsystems.walletkit.uiv3.components.tabs.TabsTopType
import io.horizontalsystems.marketkit.models.MarketGlobal
import java.math.BigDecimal

@Composable
fun MarketScreen(
    navigation: HSNavigation,
) {
    val viewModel = viewModel<MarketViewModel>(factory = MarketModule.Factory())
    val uiState = viewModel.uiState
    val tabs = viewModel.tabs

    HSScaffold(
        title = stringResource(R.string.Market_Title),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Balance_ReceiveHint_Search),
                icon = R.drawable.ic_search,
                tint = ComposeAppTheme.colors.grey,
                onClick = {
                    navigation.slideFromBottom(MarketSearchPage)
                    stat(
                        page = StatPage.Markets,
                        event = StatEvent.Open(StatPage.MarketSearch)
                    )
                }
            )
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
//                Crossfade(uiState.marketGlobal, label = "") {
//                    MetricsBoard(navigation, it, uiState.currency)
//                }
                TabsSection(navigation, tabs, uiState.selectedTab) { tab ->
                    viewModel.onSelect(tab)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabsSection(
    navigation: HSNavigation,
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
            Tab.Coins -> TopCoins(onCoinClick = { onCoinClick(it, navigation) })
            Tab.Watchlist -> MarketFavoritesScreen(navigation)
            Tab.Earn -> MarketEarnScreen(navigation)
            Tab.Posts -> MarketPostsScreen()
            Tab.Platform -> TopPlatforms(navigation)
            Tab.Pairs -> TopPairsScreen()
            Tab.Sectors -> TopSectorsScreen(navigation)
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
    navigation: HSNavigation,
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
                openMetricsPage(MetricsType.TotalMarketCap, navigation)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_VolumeShort),
            value = marketGlobal?.volume,
            changePercentage = marketGlobal?.volumeChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Volume24h, navigation)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_TvlInDefi),
            value = marketGlobal?.tvl,
            changePercentage = marketGlobal?.tvlChange,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.TvlInDefi, navigation)
            }
        )

        VDivider()

        MarketTotalCard(
            title = stringResource(R.string.MarketGlobalMetrics_EtfInflow),
            value = marketGlobal?.etfTotalInflow,
            changeFiat = marketGlobal?.etfDailyInflow,
            currency = currency,
            onClick = {
                openMetricsPage(MetricsType.Etf, navigation)
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

private fun openMetricsPage(metricsType: MetricsType, navigation: HSNavigation) {
    when (metricsType) {
        MetricsType.TvlInDefi -> {
            navigation.slideFromBottom(TvlPage)
        }

        MetricsType.Etf -> {
            navigation.slideFromBottom(EtfPage)
        }

        else -> {
            navigation.slideFromBottom(MetricsPage(metricsType))
        }
    }

    stat(page = StatPage.Markets, event = StatEvent.Open(metricsType.statPage))
}

private fun onCoinClick(coinUid: String, navigation: HSNavigation) {
    val arguments = CoinPage.Input(coinUid)

    navigation.slideFromRight(CoinPage(arguments))

    stat(page = StatPage.Markets, event = StatEvent.OpenCoin(coinUid), section = StatSection.Coins)
}
