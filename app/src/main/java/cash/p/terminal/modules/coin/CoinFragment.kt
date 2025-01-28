package cash.p.terminal.modules.coin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import io.horizontalsystems.core.getInput
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statTab
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsScreen
import cash.p.terminal.modules.coin.coinmarkets.CoinMarketsScreen
import cash.p.terminal.modules.coin.overview.ui.CoinOverviewScreen
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui.compose.components.ListEmptyView
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.CoinFragmentInput
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.TabItem
import cash.p.terminal.ui_compose.components.Tabs
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<CoinFragmentInput>()
        val coinUid = input?.coinUid ?: ""

        CoinScreen(
            coinUid,
            coinViewModel(coinUid),
            navController,
            childFragmentManager
        )
    }

    private fun coinViewModel(coinUid: String): CoinViewModel? = try {
        val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) {
            CoinModule.Factory(coinUid)
        }
        viewModel
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CoinScreen(
    coinUid: String,
    coinViewModel: CoinViewModel?,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    if (coinViewModel != null) {
        CoinTabs(coinViewModel, navController, fragmentManager)
    } else {
        CoinNotFound(coinUid, navController)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoinTabs(
    viewModel: CoinViewModel,
    navController: NavController,
    fragmentManager: FragmentManager
) {
    val tabs = viewModel.tabs
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = viewModel.fullCoin.coin.code,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = buildList {
                    if (viewModel.isWatchlistEnabled) {
                        if (viewModel.isFavorite) {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.CoinPage_Unfavorite),
                                    icon = R.drawable.ic_filled_star_24,
                                    tint = ComposeAppTheme.colors.jacob,
                                    onClick = {
                                        viewModel.onUnfavoriteClick()

                                        stat(
                                            page = StatPage.CoinPage,
                                            event = StatEvent.RemoveFromWatchlist(viewModel.fullCoin.coin.uid)
                                        )
                                    }
                                )
                            )
                        } else {
                            add(
                                MenuItem(
                                    title = TranslatableString.ResString(R.string.CoinPage_Favorite),
                                    icon = R.drawable.ic_star_24,
                                    onClick = {
                                        viewModel.onFavoriteClick()

                                        stat(
                                            page = StatPage.CoinPage,
                                            event = StatEvent.AddToWatchlist(viewModel.fullCoin.coin.uid)
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            )
        }
    ) { innerPaddings ->
        Column(
            modifier = Modifier.padding(innerPaddings)
        ) {
            val selectedTab = tabs[pagerState.currentPage]
            val tabItems = tabs.map {
                TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
            }
            Tabs(tabItems, onClick = { tab ->
                coroutineScope.launch {
                    pagerState.scrollToPage(tab.ordinal)

                    stat(page = StatPage.CoinPage, event = StatEvent.SwitchTab(tab.statTab))

                    if (tab == CoinModule.Tab.Details && viewModel.shouldShowSubscriptionInfo()) {
                        viewModel.subscriptionInfoShown()

                        delay(1000)
                        navController.slideFromBottom(R.id.subscriptionInfoFragment)
                    }
                }
            })

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    CoinModule.Tab.Overview -> {
                        CoinOverviewScreen(
                            fullCoin = viewModel.fullCoin,
                            navController = navController
                        )
                    }

                    CoinModule.Tab.Market -> {
                        CoinMarketsScreen(fullCoin = viewModel.fullCoin)
                    }

                    CoinModule.Tab.Details -> {
                        CoinAnalyticsScreen(
                            fullCoin = viewModel.fullCoin,
                            navController = navController,
                            fragmentManager = fragmentManager
                        )
                    }
                }
            }

            viewModel.successMessage?.let {
                HudHelper.showSuccessMessage(view, it)

                viewModel.onSuccessMessageShown()
            }
        }
    }
}

@Composable
fun CoinNotFound(coinUid: String, navController: NavController) {
    Scaffold(
        backgroundColor = cash.p.terminal.ui_compose.theme.ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = coinUid,
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                }
            )
        },
        content = {
            ListEmptyView(
                paddingValues = it,
                text = stringResource(R.string.CoinPage_CoinNotFound, coinUid),
                icon = R.drawable.ic_not_available
            )
        }
    )
}
