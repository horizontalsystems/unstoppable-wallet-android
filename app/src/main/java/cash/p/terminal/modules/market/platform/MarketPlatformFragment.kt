package cash.p.terminal.modules.market.platform

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.getInput
import cash.p.terminal.core.slideFromRight
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statSortType
import io.horizontalsystems.core.entities.ViewState
import io.horizontalsystems.chartview.chart.ChartViewModel
import cash.p.terminal.ui_compose.CoinFragmentInput
import io.horizontalsystems.chartview.ui.Chart
import cash.p.terminal.modules.coin.overview.ui.Loading
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.modules.market.topcoins.OptionController
import cash.p.terminal.modules.market.topplatforms.Platform
import cash.p.terminal.ui.compose.HSSwipeRefresh
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui.compose.components.CoinList
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HeaderSorting
import cash.p.terminal.ui.compose.components.ListErrorView
import cash.p.terminal.ui.compose.components.TopCloseButton
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.title3_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

class MarketPlatformFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {

        val platform = navController.getInput<Platform>()

        if (platform == null) {
            navController.popBackStack()
            return
        }

        val factory = MarketPlatformModule.Factory(platform)

        PlatformScreen(
            factory = factory,
            onCloseButtonClick = { navController.popBackStack() },
            onCoinClick = { coinUid ->
                val arguments = CoinFragmentInput(coinUid)
                navController.slideFromRight(R.id.coinFragment, arguments)

                stat(page = StatPage.TopPlatform, event = StatEvent.OpenCoin(coinUid))
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlatformScreen(
    factory: ViewModelProvider.Factory,
    onCloseButtonClick: () -> Unit,
    onCoinClick: (String) -> Unit,
    viewModel: MarketPlatformViewModel = viewModel(factory = factory),
    chartViewModel: ChartViewModel = viewModel(factory = factory),
) {

    val uiState = viewModel.uiState
    var scrollToTopAfterUpdate by rememberSaveable { mutableStateOf(false) }
    var openSortingSelector by rememberSaveable { mutableStateOf(false) }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(onCloseButtonClick)

            HSSwipeRefresh(
                refreshing = uiState.isRefreshing,
                onRefresh = {
                    viewModel.refresh()

                    stat(page = StatPage.TopPlatform, event = StatEvent.Refresh)
                }
            ) {
                Crossfade(uiState.viewState, label = "") { state ->
                    when (state) {
                        ViewState.Loading -> {
                            Loading()
                        }

                        is ViewState.Error -> {
                            ListErrorView(
                                stringResource(R.string.SyncError),
                                viewModel::onErrorClick
                            )
                        }

                        ViewState.Success -> {
                            uiState.viewItems.let { viewItems ->
                                CoinList(
                                    items = viewItems,
                                    scrollToTop = scrollToTopAfterUpdate,
                                    onAddFavorite = { uid ->
                                        viewModel.onAddFavorite(uid)

                                        stat(
                                            page = StatPage.TopPlatform,
                                            event = StatEvent.AddToWatchlist(uid)
                                        )
                                    },
                                    onRemoveFavorite = { uid ->
                                        viewModel.onRemoveFavorite(uid)

                                        stat(
                                            page = StatPage.TopPlatform,
                                            event = StatEvent.RemoveFromWatchlist(uid)
                                        )
                                    },
                                    onCoinClick = onCoinClick,
                                    preItems = {
                                        viewModel.header.let {
                                            item {
                                                HeaderContent(it.title, it.description, it.icon)
                                            }
                                        }
                                        item {
                                            Chart(
                                                uiState = chartViewModel.uiState,
                                                getSelectedPointCallback = chartViewModel::getSelectedPoint,
                                                onSelectChartInterval = chartViewModel::onSelectChartInterval)
                                        }
                                        stickyHeader {
                                            HeaderSorting(borderTop = true, borderBottom = true) {
                                                HSpacer(width = 16.dp)
                                                OptionController(
                                                    uiState.sortingField.titleResId,
                                                    onOptionClick = {
                                                        openSortingSelector = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                                if (scrollToTopAfterUpdate) {
                                    scrollToTopAfterUpdate = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (openSortingSelector) {
        AlertGroup(
            R.string.Market_Sort_PopupTitle,
            Select(uiState.sortingField, viewModel.sortingFields),
            { selected ->
                scrollToTopAfterUpdate = true
                viewModel.onSelectSortingField(selected)
                openSortingSelector = false
                stat(
                    page = StatPage.TopPlatform,
                    event = StatEvent.SwitchSortType(selected.statSortType)
                )
            },
            { openSortingSelector = false }
        )
    }
}

@Composable
private fun HeaderContent(title: String, description: String, image: ImageSource) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(ComposeAppTheme.colors.tyler)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .weight(1f)
        ) {
            title3_leah(
                text = title,
            )
            subhead2_grey(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Image(
            painter = image.painter(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 24.dp)
                .size(32.dp),
        )
    }
}

@Preview
@Composable
fun HeaderContentPreview() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        HeaderContent(
            "Solana Ecosystem",
            "Market cap of all protocols on the Solana chain",
            ImageSource.Local(R.drawable.logo_ethereum_24)
        )
    }
}
